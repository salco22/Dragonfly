package com.salco.login.feature.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.salco.login.feature.viewmodel.LoginViewModel
import com.salco.navigation.AppComposeNavigator
import com.salco.navigation.DragonflyAppNavigator

@Composable
fun LoginScreen(
    composeNavigator: AppComposeNavigator,
    viewModel: LoginViewModel = viewModel()
){
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ){
        Text(text = "Email")
        OutlinedTextField(
            modifier = Modifier.padding(horizontal = 15.dp).fillMaxWidth(),
            value = "",
            onValueChange = {}
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Password")
        OutlinedTextField(
            modifier = Modifier.padding(horizontal = 15.dp).fillMaxWidth(),
            value = "",
            onValueChange = {}
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp)
        ) {
            Row {
                Checkbox(
                    checked = false,
                    onCheckedChange = {}
                )
                Text(text = "Ricordami")
            }
            Text(
                text = "Password dimenticata"
            )
        }
        Button(
            modifier = Modifier
                .padding(horizontal = 15.dp, vertical = 30.dp)
                .fillMaxWidth(),
            onClick = {}
        ) {
            Text(text = "Login")
        }
    }
}


@Preview(showBackground = true)
@Composable
internal fun LoginScreenPreview(){
    LoginScreen(
        composeNavigator = DragonflyAppNavigator()
    )
}