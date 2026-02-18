package com.Linkbyte.Shift.presentation.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onSignUpSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
 
    var currentStep by rememberSaveable { mutableStateOf(0) }
 
    var displayName by rememberSaveable { mutableStateOf("") }
    var selectedDay by rememberSaveable { mutableIntStateOf(0) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(0) }
    var selectedYear by rememberSaveable { mutableIntStateOf(0) }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    
    var isTermsAccepted by rememberSaveable { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var ageError by rememberSaveable { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onSignUpSuccess()
        }
    }

    fun validateAge(): Boolean {
        return try {
            if (selectedDay == 0 || selectedMonth == 0 || selectedYear == 0) {
                ageError = "Please select your full birthday."
                return false
            }

            val birthDate = LocalDate.of(selectedYear, selectedMonth, selectedDay)
            val now = LocalDate.now()
            val age = ChronoUnit.YEARS.between(birthDate, now)

            if (age < 13) {
                ageError = "You must be at least 13 years old to use Shift."
                false
            } else {
                ageError = null
                true
            }
        } catch (e: Exception) {
            ageError = "Please select a valid date."
            false
        }
    }

    fun onBack() {
        if (currentStep > 0) {
            currentStep--
            ageError = null
        } else {
            onNavigateToLogin()
        }
    }

    fun onContinue() {
        when (currentStep) {
            0 -> if (displayName.isNotBlank()) currentStep++
            1 -> if (validateAge()) currentStep++
            2 -> if (username.isNotBlank() && username.length >= 3) currentStep++
            3 -> if (email.isNotBlank() && email.contains("@")) currentStep++
            4 -> if (password.length >= 6) {
                viewModel.signUp(email, password, username, displayName)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Top Navigation & Progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { onBack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }

            if (currentStep < 5) {
                Text(
                    text = "Step ${currentStep + 1} of 5",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Progress indicator
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (index <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }

        // Main Wizard Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "SignUpWizard"
            ) { step ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (step) {
                        0 -> {
                            StepHeader("What's your name?", "This is how you'll appear on Shift.")
                            Spacer(modifier = Modifier.height(32.dp))
                            ShiftTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = "Display Name",
                                placeholder = "First & Last Name",
                                autoFocus = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { onContinue() })
                            )
                        }
                        1 -> {
                            StepHeader("When's your birthday?", "To ensure you're old enough to use Shift.")
                            Spacer(modifier = Modifier.height(32.dp))

                            val months = listOf(
                                "January", "February", "March", "April", "May", "June",
                                "July", "August", "September", "October", "November", "December"
                            )
                            val currentYear = LocalDate.now().year
                            val years = (currentYear downTo currentYear - 100).toList()
                            val maxDay = if (selectedMonth > 0 && selectedYear > 0) {
                                try {
                                    LocalDate.of(selectedYear, selectedMonth, 1).lengthOfMonth()
                                } catch (e: Exception) { 31 }
                            } else 31
                            val days = (1..maxDay).toList()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ShiftDropdown(
                                    label = "Month",
                                    selectedText = if (selectedMonth > 0) months[selectedMonth - 1] else "Month",
                                    items = months,
                                    onItemSelected = { index -> selectedMonth = index + 1 },
                                    modifier = Modifier.weight(1.4f)
                                )
                                ShiftDropdown(
                                    label = "Day",
                                    selectedText = if (selectedDay > 0) selectedDay.toString() else "Day",
                                    items = days.map { it.toString() },
                                    onItemSelected = { index -> selectedDay = days[index] },
                                    modifier = Modifier.weight(0.8f)
                                )
                                ShiftDropdown(
                                    label = "Year",
                                    selectedText = if (selectedYear > 0) selectedYear.toString() else "Year",
                                    items = years.map { it.toString() },
                                    onItemSelected = { index -> selectedYear = years[index] },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (ageError != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(ageError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        2 -> {
                            StepHeader("Pick a username", "Your unique identity on Shift.")
                            Spacer(modifier = Modifier.height(32.dp))
                            ShiftTextField(
                                value = username,
                                onValueChange = { username = it.take(20).trim() },
                                label = "Username",
                                placeholder = "@username",
                                autoFocus = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { onContinue() })
                            )
                        }
                        3 -> {
                            StepHeader("What's your email?", "We'll use this for account recovery.")
                            Spacer(modifier = Modifier.height(32.dp))
                            ShiftTextField(
                                value = email,
                                onValueChange = { email = it.trim() },
                                label = "Email",
                                placeholder = "name@example.com",
                                autoFocus = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { onContinue() })
                            )
                        }
                        4 -> { // Final Step: Password & Legal
                            StepHeader("Set a password", "Make it strong and secure.")
                            Spacer(modifier = Modifier.height(32.dp))
                            ShiftTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = "Password",
                                placeholder = "Min 6 chars",
                                autoFocus = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { 
                                     if (password.length >= 6 && isTermsAccepted) onContinue()
                                })
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isTermsAccepted,
                                    onCheckedChange = { isTermsAccepted = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "I agree to the Terms of Service and Privacy Policy",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row {
                                        Text(
                                            text = "Terms of Service",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { onNavigateToTerms() }
                                        )
                                        Text(" and ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            text = "Privacy Policy",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { onNavigateToPrivacy() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            ShiftButton(
                text = if (currentStep == 4) "Sign Up" else "Continue",
                onClick = { onContinue() },
                modifier = Modifier.fillMaxWidth(),
                enabled = when(currentStep) {
                    0 -> displayName.isNotBlank()
                    1 -> selectedDay > 0 && selectedMonth > 0 && selectedYear > 0
                    2 -> username.isNotBlank() && username.length >= 3
                    3 -> email.isNotBlank() && email.contains("@")
                    4 -> password.length >= 6 && isTermsAccepted
                    else -> false
                } && uiState !is AuthUiState.Loading,
                isLoading = uiState is AuthUiState.Loading,
                variant = ButtonVariant.Primary
            )
        }
    }

        if (currentStep == 0) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ShiftButton(
                    text = "Sign In",
                    onClick = onNavigateToLogin,
                    variant = ButtonVariant.Ghost
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            TextButton(
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:linkbytedevelopmentsolutions@gmail.com")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Shift Support - Sign Up Issue")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fail silent
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Need help? Contact support",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentBlue
                )
            }
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftDropdown(
    label: String,
    selectedText: String,
    items: List<String>,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedText == label) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .heightIn(max = 250.dp)
        ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onItemSelected(index)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
