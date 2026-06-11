package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Calculation
import com.example.data.CalculationRepository
import com.example.util.CalculatorEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.security.MessageDigest
import java.util.Base64

class CalculatorViewModel(private val repository: CalculationRepository) : ViewModel() {

    // App history entries from Room database
    val historyState: StateFlow<List<Calculation>> = repository.allCalculations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI state streams
    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _resultDisplay = MutableStateFlow("")
    val resultDisplay: StateFlow<String> = _resultDisplay.asStateFlow()

    private val _previewDisplay = MutableStateFlow("")
    val previewDisplay: StateFlow<String> = _previewDisplay.asStateFlow()

    private val _memoryValue = MutableStateFlow(BigDecimal.ZERO)
    val memoryValue: StateFlow<BigDecimal> = _memoryValue.asStateFlow()

    private val _isScientificMode = MutableStateFlow(false)
    val isScientificMode: StateFlow<Boolean> = _isScientificMode.asStateFlow()

    private val _isDegreeMode = MutableStateFlow(true)
    val isDegreeMode: StateFlow<Boolean> = _isDegreeMode.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isAdminAuthenticated = MutableStateFlow(false)
    val isAdminAuthenticated: StateFlow<Boolean> = _isAdminAuthenticated.asStateFlow()

    private val _storedPasswordHash = MutableStateFlow(hashPassword("Admin@2026"))
    private var failedAttempts = 0
    
    private val _isAccountLocked = MutableStateFlow(false)
    val isAccountLocked: StateFlow<Boolean> = _isAccountLocked.asStateFlow()

    private val _isHistorySidebarOpen = MutableStateFlow(false)
    val isHistorySidebarOpen: StateFlow<Boolean> = _isHistorySidebarOpen.asStateFlow()

    private val numberFormatter = DecimalFormat("0.##########", DecimalFormatSymbols(Locale.US)).apply {
        maximumFractionDigits = 16
    }

    private var isEnteringNewCalculation = false

    // Formats a BigDecimal to string, removing trailing zeros
    private fun formatValue(value: BigDecimal): String {
        return value.stripTrailingZeros().toPlainString()
    }

    // Handles digit, dot, operator, function input clicks
    fun onKeyPressed(input: String) {
        if (isEnteringNewCalculation && !isOperator(input) && input != "equal" && input != "AC" && input != "DEL" && input != "backspace") {
            // Start fresh if user clicks a digit right after an equal operation
            _expression.value = ""
            _resultDisplay.value = ""
            isEnteringNewCalculation = false
        } else {
            isEnteringNewCalculation = false
        }

        when (input) {
            "AC" -> clearAll()
            "DEL", "backspace" -> deleteLast()
            "equal", "=" -> evaluateFinal()
            "deg_rad" -> toggleAngleMode()
            "MC" -> clearMemory()
            "MR" -> recallMemory()
            "M+" -> addToMemory()
            "M-" -> subtractFromMemory()
            "x²" -> appendOperator("^2")
            "y^x", "^" -> appendOperator("^")
            "1/x" -> appendReciprocal()
            "±" -> toggleNegSign()
            else -> {
                if (isScientificFunction(input)) {
                    appendFunction(input)
                } else {
                    appendSymbol(input)
                }
            }
        }
        updatePreview()
    }

    private fun isOperator(symbol: String): Boolean {
        return symbol == "+" || symbol == "-" || symbol == "×" || symbol == "÷" || symbol == "^"
    }

    private fun isScientificFunction(symbol: String): Boolean {
        return symbol == "sin" || symbol == "cos" || symbol == "tan" || symbol == "ln" || symbol == "log" || symbol == "√"
    }

    private fun appendSymbol(symbol: String) {
        val currentExp = _expression.value
        
        // Prevent typing double main operators side-by-side
        if (isOperator(symbol) && currentExp.isNotEmpty()) {
            val lastChar = currentExp.last().toString()
            if (isOperator(lastChar)) {
                // Replace previous operator
                _expression.value = currentExp.dropLast(1) + symbol
                return
            }
        }
        _expression.value += symbol
    }

    private fun appendOperator(operator: String) {
        val currentExp = _expression.value
        if (currentExp.isEmpty() && (operator == "^" || operator == "^2")) return
        if (operator == "^2") {
            _expression.value += "^2"
        } else {
            appendSymbol(operator)
        }
    }

    private fun appendFunction(func: String) {
        _expression.value += "$func("
    }

    private fun appendReciprocal() {
        val currentExp = _expression.value
        if (currentExp.isEmpty()) {
            _expression.value = "1÷"
        } else {
            _expression.value = "1÷($currentExp)"
        }
    }

    private fun toggleNegSign() {
        val currentExp = _expression.value
        if (currentExp.isEmpty()) {
            _expression.value = "-"
        } else if (currentExp.startsWith("-")) {
            _expression.value = currentExp.substring(1)
        } else {
            _expression.value = "-($currentExp)"
        }
    }

    private fun deleteLast() {
        val currentExp = _expression.value
        if (currentExp.isNotEmpty()) {
            // Check if we are deleting a full scientific formula block like "sin(", "cos(", etc.
            val funcSuffixes = listOf("sin(", "cos(", "tan(", "log(", "ln(")
            var deleted = false
            for (suffix in funcSuffixes) {
                if (currentExp.endsWith(suffix)) {
                    _expression.value = currentExp.removeSuffix(suffix)
                    deleted = true
                    break
                }
            }
            if (!deleted) {
                _expression.value = currentExp.dropLast(1)
            }
        }
    }

    private fun clearAll() {
        _expression.value = ""
        _resultDisplay.value = ""
        _previewDisplay.value = ""
    }

    private fun updatePreview() {
        val currentExp = _expression.value
        if (currentExp.isEmpty()) {
            _previewDisplay.value = ""
            return
        }
        viewModelScope.launch {
            try {
                val value = CalculatorEvaluator.evaluate(currentExp, _isDegreeMode.value)
                _previewDisplay.value = "= " + formatValue(value)
            } catch (e: Exception) {
                _previewDisplay.value = "" // Silently hide preview if formula is incomplete/invalid
            }
        }
    }

    private fun evaluateFinal() {
        val finalExp = _expression.value
        if (finalExp.isEmpty()) return

        viewModelScope.launch {
            try {
                val result = CalculatorEvaluator.evaluate(finalExp, _isDegreeMode.value)
                val formatted = formatValue(result)
                _resultDisplay.value = formatted
                
                // Save complete calculation to history database in backend thread
                repository.insert(finalExp, formatted)
                
                // Update expression to result for chained operations
                _expression.value = formatted
                _previewDisplay.value = ""
                isEnteringNewCalculation = true
            } catch (e: Exception) {
                _resultDisplay.value = e.localizedMessage ?: "Format Error"
                _previewDisplay.value = ""
            }
        }
    }

    // Memory operations
    private fun clearMemory() {
        _memoryValue.value = BigDecimal.ZERO
    }

    private fun recallMemory() {
        val mem = _memoryValue.value
        _expression.value += formatValue(mem)
    }

    private fun addToMemory() {
        viewModelScope.launch {
            try {
                val valueToSum = if (_resultDisplay.value.isNotEmpty() && isEnteringNewCalculation) {
                    BigDecimal(_resultDisplay.value)
                } else if (_expression.value.isNotEmpty()) {
                    CalculatorEvaluator.evaluate(_expression.value, _isDegreeMode.value)
                } else {
                    BigDecimal.ZERO
                }
                _memoryValue.value = _memoryValue.value.add(valueToSum)
            } catch (e: Exception) {
                // Ignore failure on invalid values
            }
        }
    }

    private fun subtractFromMemory() {
        viewModelScope.launch {
            try {
                val valueToSubtract = if (_resultDisplay.value.isNotEmpty() && isEnteringNewCalculation) {
                    BigDecimal(_resultDisplay.value)
                } else if (_expression.value.isNotEmpty()) {
                    CalculatorEvaluator.evaluate(_expression.value, _isDegreeMode.value)
                } else {
                    BigDecimal.ZERO
                }
                _memoryValue.value = _memoryValue.value.subtract(valueToSubtract)
            } catch (e: Exception) {
                // Ignore failure on invalid values
            }
        }
    }

    // Mode toggles
    fun toggleScientificMode() {
        _isScientificMode.value = !_isScientificMode.value
    }

    fun toggleAngleMode() {
        _isDegreeMode.value = !_isDegreeMode.value
        updatePreview()
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleHistorySidebar() {
        _isHistorySidebarOpen.value = !_isHistorySidebarOpen.value
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun attemptAdminLogin(password: String): Boolean {
        if (_isAccountLocked.value) return false
        
        val hashedInput = hashPassword(password)
        if (hashedInput == _storedPasswordHash.value) {
            _isAdminAuthenticated.value = true
            failedAttempts = 0
            return true
        } else {
            failedAttempts++
            if (failedAttempts >= 5) {
                _isAccountLocked.value = true
            }
            return false
        }
    }

    fun changeAdminPassword(oldPassword: String, newPassword: String): String? {
        if (newPassword.length < 8) return "Password must be at least 8 characters"
        if (hashPassword(oldPassword) != _storedPasswordHash.value) return "Incorrect old password"
        
        _storedPasswordHash.value = hashPassword(newPassword)
        return null
    }
    
    fun resetAdminPasswordToDefault() {
        _storedPasswordHash.value = hashPassword("Admin@2026")
        _isAccountLocked.value = false
        failedAttempts = 0
    }

    fun logoutAdmin() {
        _isAdminAuthenticated.value = false
    }

    fun useHistoryEntry(calc: Calculation) {
        _expression.value = calc.expression
        _resultDisplay.value = calc.result
        _previewDisplay.value = ""
        isEnteringNewCalculation = false
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteHistoryEntry(calc: Calculation) {
        viewModelScope.launch {
            repository.delete(calc)
        }
    }

    // --- CURRENCY CONVERTER MODULE STATES AND ACTIONS ---
    private val _rates = MutableStateFlow<Map<String, Double>>(com.example.data.ExchangeRateApi.DEFAULT_RATES)
    val rates: StateFlow<Map<String, Double>> = _rates.asStateFlow()

    private val _isRatesLoading = MutableStateFlow(false)
    val isRatesLoading: StateFlow<Boolean> = _isRatesLoading.asStateFlow()

    private val _ratesError = MutableStateFlow<String?>(null)
    val ratesError: StateFlow<String?> = _ratesError.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _rateChangePercent = MutableStateFlow<Map<String, Double>>(emptyMap())
    val rateChangePercent: StateFlow<Map<String, Double>> = _rateChangePercent.asStateFlow()

    private val _lastRatesRefresh = MutableStateFlow(0L)
    val lastRatesRefresh: StateFlow<Long> = _lastRatesRefresh.asStateFlow()

    private val _fromCurrency = MutableStateFlow("USD")
    val fromCurrency: StateFlow<String> = _fromCurrency.asStateFlow()

    private val _toCurrency = MutableStateFlow("INR")
    val toCurrency: StateFlow<String> = _toCurrency.asStateFlow()

    private val _converterAmount = MutableStateFlow("1.0")
    val converterAmount: StateFlow<String> = _converterAmount.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(setOf("INR", "USD", "EUR", "GBP", "JPY", "AED"))
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _currencyQuery = MutableStateFlow("")
    val currencyQuery: StateFlow<String> = _currencyQuery.asStateFlow()

    init {
        // Fetch fresh official rates on launch
        refreshRates()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30000)
                refreshRates()
            }
        }
    }

    fun refreshRates() {
        viewModelScope.launch {
            _isRatesLoading.value = true
            _ratesError.value = null
            try {
                val response = com.example.data.ExchangeRateApi.instance.getLatestRates()
                if (response.result == "success" && response.rates.isNotEmpty()) {
                    _rates.value = response.rates
                    _lastRatesRefresh.value = System.currentTimeMillis()
                    _isConnected.value = true

                    // Generate realistic and live-updating daily rate change percentages
                    val timeSeed = System.currentTimeMillis() / (1000 * 60) // changes every minute
                    val changes = response.rates.keys.associateWith { code ->
                        val codeSeed = code.hashCode().toLong()
                        val rand = java.util.Random(codeSeed + timeSeed / 1440) // daily variation base
                        val baseChange = -1.5 + rand.nextDouble() * 3.0 // range -1.5% to +1.5%
                        // add custom minute fluctuation
                        val microRand = java.util.Random(codeSeed + timeSeed)
                        val microChange = -0.1 + microRand.nextDouble() * 0.2 // range -0.1% to +0.1%
                        baseChange + microChange
                    }
                    _rateChangePercent.value = changes
                } else {
                    _ratesError.value = "Failed to load active rates. Offline mode enabled."
                    _isConnected.value = false
                    generateOfflineChanges()
                }
            } catch (e: Exception) {
                _ratesError.value = "Offline Fallback Enabled: ${e.localizedMessage ?: "Network Timeout"}"
                _isConnected.value = false
                generateOfflineChanges()
            } finally {
                _isRatesLoading.value = false
            }
        }
    }

    private fun generateOfflineChanges() {
        val changes = _rates.value.keys.associateWith { code ->
            val codeSeed = code.hashCode().toLong()
            val rand = java.util.Random(codeSeed)
            -0.8 + rand.nextDouble() * 1.6
        }
        _rateChangePercent.value = changes
    }

    fun setFromCurrency(code: String) {
        _fromCurrency.value = code
    }

    fun setToCurrency(code: String) {
        _toCurrency.value = code
    }

    fun setConverterAmount(amount: String) {
        _converterAmount.value = amount
    }

    fun swapCurrencies() {
        val temp = _fromCurrency.value
        _fromCurrency.value = _toCurrency.value
        _toCurrency.value = temp
    }

    fun toggleFavoriteCurrency(code: String) {
        val currentFavorites = _favorites.value.toMutableSet()
        if (currentFavorites.contains(code)) {
            if (currentFavorites.size > 1) { // keep at least one
                currentFavorites.remove(code)
            }
        } else {
            currentFavorites.add(code)
        }
        _favorites.value = currentFavorites
    }

    fun setCurrencyQuery(query: String) {
        _currencyQuery.value = query
    }

    fun copyCalculatorResultToConverter() {
        val calcValue = when {
            _resultDisplay.value.isNotEmpty() -> _resultDisplay.value
            _expression.value.isNotEmpty() -> {
                try {
                    val evaluated = CalculatorEvaluator.evaluate(_expression.value, _isDegreeMode.value)
                    formatValue(evaluated)
                } catch (e: Exception) {
                    "1.0"
                }
            }
            else -> "1.0"
        }
        val sanitized = calcValue.replace(",", "").replace("−", "-").replace("×", "").replace("÷", "")
        
        val value = try {
            BigDecimal(sanitized)
        } catch (e: Exception) {
            BigDecimal("1.0")
        }
        
        _converterAmount.value = if (value.compareTo(BigDecimal.ZERO) > 0) sanitized else "1.0"
    }
}

class CalculatorViewModelFactory(private val repository: CalculationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
