package io.github.chisarabivorts.tessera.sample.checkout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.chisarabivorts.tessera.Navigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmScreen(navigator: Navigator) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Оформление · Шаг 2 из 2 · Подтверждение") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Заказ подтверждён.",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "По кнопке «Готово» весь вложенный граф 'checkout' снимается из " +
                    "back stack, и мы сразу попадаем на 'home'. Реализовано через " +
                    "navigator.popBackStackTo(route = \"home\").",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // popBackStackTo - закрывает оба экрана (Confirm + Address)
                    // и возвращает пользователя на таб «Главная».
                    navigator.popBackStackTo(route = "home")
                },
            ) {
                Text("Готово, на Главную")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navigator.popBackStack() },
            ) {
                Text("Назад к адресу")
            }
        }
    }
}
