package com.example.hurricansolutionapp

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.net.ConnectException
import java.net.SocketTimeoutException

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.hurricane_solution_blanco),
                contentDescription = "Logo Hurricane Solution",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .height(120.dp)
                    .padding(bottom = 18.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xB10F1116),
                tonalElevation = 0.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Bienvenido de nuevo",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "Inicia sesión para continuar",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 0.dp)
                    )

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.12f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.85f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedLabelColor = Color.White.copy(alpha = 0.85f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.85f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedLabelColor = Color.White.copy(alpha = 0.85f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            errorMsg = null

                            if (!SessionManager.isLoggedIn(context) && !isOnline(context)) {
                                errorMsg = "No hay conexión a internet. Conéctate para iniciar sesión."
                                return@Button
                            }

                            if (correo.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Ingresa correo y contraseña.", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            scope.launch {
                                try {
                                    loading = true
                                    Log.d(TAG, "Iniciando login para: $correo")

                                    val user = AuthRepository.login(correo, password)
                                    Log.d(TAG, "Login exitoso: ${user.nombre}, role: ${user.role}")

                                    SessionManager.login(
                                        context = context,
                                        userId = user.userId,
                                        nombre = user.nombre,
                                        role = user.role
                                    )

                                    Toast.makeText(context, "Bienvenido ${user.nombre}", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()

                                } catch (e: Exception) {
                                    // LOG DEL ERROR REAL
                                    Log.e(TAG, "Error en login: ${e.javaClass.simpleName}: ${e.message}", e)

                                    val msg = (e.message ?: "").lowercase()

                                    val userFriendly = when {
                                        msg.contains("invalid login credentials") ||
                                                msg.contains("invalid_credentials") ||
                                                msg.contains("invalid_grant") -> "Usuario o contraseña incorrectos."

                                        msg.contains("usuario desactivado") ||
                                                msg.contains("usuario inactivo") -> "Tu usuario está inactivo. Contacta al administrador."

                                        msg.contains("row-level security") ||
                                                msg.contains("rls") ||
                                                msg.contains("policy") -> "Error de permisos en la base de datos. Contacta al administrador."

                                        msg.contains("no rows") ||
                                                msg.contains("not found") ||
                                                msg.contains("0 rows") -> "No se encontró el perfil del usuario. Contacta al administrador."

                                        e is UnknownHostException ||
                                                e is ConnectException ||
                                                e is SocketTimeoutException ||
                                                msg.contains("unable to resolve host") ||
                                                msg.contains("failed to connect") ||
                                                msg.contains("timeout") ||
                                                msg.contains("http request") -> "No hay conexión a internet. Verifica tu red e intenta de nuevo."

                                        else -> {
                                            // Mostrar error técnico para diagnóstico
                                            "Error: ${e.message?.take(100) ?: "Desconocido"}"
                                        }
                                    }

                                    errorMsg = userFriendly

                                } finally {
                                    loading = false
                                }
                            }
                        },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(top = 2.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6E6E6),
                            contentColor = Color(0xFF0C0F18),
                            disabledContainerColor = Color(0xFFBDBDBD),
                            disabledContentColor = Color(0xFF0C0F18)
                        )
                    ) {
                        Text(
                            text = if (loading) "ENTRANDO..." else "INICIAR SESIÓN",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text(
                        text = "¿Problemas para iniciar sesión?\nContacta al administrador.",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}