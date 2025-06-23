package com.app.loginregister.ui.register.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.app.loginregister.BuildConfig
import com.app.loginregister.R
import com.app.loginregister.base.BaseActivity
import com.app.loginregister.base.BaseResponse
import com.app.loginregister.network.utility.RegisterError
import com.app.loginregister.network.utility.ResponseData
import com.app.loginregister.ui.register.viewmodel.RegisterViewModel
import com.app.loginregister.ui.theme.LoginRegisterTheme
import com.app.loginregister.ui.widget.CustomBottomSheetGallery
import com.app.loginregister.ui.widget.CustomDialog
import com.app.loginregister.ui.widget.ImagePickerBottomSheet
import com.app.loginregister.ui.widget.MarginVertical
import com.app.loginregister.ui.widget.MyEditTextField
import com.app.loginregister.ui.widget.MyText
import com.app.loginregister.ui.widget.ShowLoader
import com.app.loginregister.ui.widget.ShowMyDialog
import com.app.loginregister.util.Method
import com.app.loginregister.util.PathUtil
import com.app.loginregister.util.emailKeyBord
import com.app.loginregister.util.noRippleClickable
import com.app.loginregister.util.openSetting
import com.app.loginregister.util.passwordKeyBord
import com.app.loginregister.util.style.text14Regular
import com.app.loginregister.util.style.text22Bold
import com.app.loginregister.util.textKeyBord
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : BaseActivity() {

    //Full name
    private var name by mutableStateOf("")
    private var nameError by mutableStateOf("")

    //Email
    private var email by mutableStateOf("")
    private var emailError by mutableStateOf("")

    //Password
    private var password by mutableStateOf("")
    private var passwordError by mutableStateOf("")

    private lateinit var registerViewModel: RegisterViewModel

    //Show loading
    private var isShowLoading by mutableStateOf(false)

    private var isImagePickDialog by mutableStateOf(false)

    //Show dialog
    private var isShowDialog by mutableStateOf(false)
    private var dialogMSG: String = ""
    private var imagePath by mutableStateOf("")

    private var isShowDialogSetting by mutableStateOf(false)

    private var cameraImagePath: String? = null

    private var isGallery = false

    private var isShowImagePicker by mutableStateOf(false)

    private lateinit var launcherPermissionMultiple: ActivityResultLauncher<Array<String>>

    private var uri = mutableListOf<Uri?>()


    @Inject
    lateinit var method: Method

    @RequiresApi(Build.VERSION_CODES.N)
    @OptIn(ExperimentalGlideComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerViewModel = ViewModelProvider(this)[RegisterViewModel::class.java]

        lifecycleScope.launch {
            registerViewModel.registerStateFlow.collect {
                when (it) {

                    is ResponseData.Success -> {
                        isShowLoading = false
                        if (it.data is BaseResponse) {
                            val baseResponse = it.data
                            if (baseResponse.status) {
                                finish()
                                Toast.makeText(
                                    this@RegisterActivity, baseResponse.message, Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                dialogMSG = baseResponse.message
                                isShowDialog = true
                            }
                        }
                    }

                    is ResponseData.Loading -> {
                        isShowLoading = true
                    }

                    is ResponseData.Error -> {

                        isShowLoading = false
                        val data = it.data
                        when (data) {

                            RegisterError.ENTER_FULL_NAME -> {
                                nameError = it.error
                            }

                            RegisterError.ENTER_EMAIL -> {
                                emailError = it.error
                            }

                            RegisterError.ENTER_VALID_EMAIL -> {
                                emailError = it.error
                            }

                            RegisterError.ENTER_PASSWORD -> {
                                passwordError = it.error
                            }

                            else -> {
                                Toast.makeText(this@RegisterActivity, it.error, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }

                    }

                    is ResponseData.InternetConnection -> {
                        Toast.makeText(this@RegisterActivity, it.error, Toast.LENGTH_SHORT).show()
                    }

                    is ResponseData.Empty -> {}
                    is ResponseData.Exception -> {
                        isShowLoading = false
                        Toast.makeText(this@RegisterActivity, it.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        setContent {
            LoginRegisterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 20.dp, end = 20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(80.dp))
                        ConstraintLayout(
                            modifier = Modifier
                                .noRippleClickable {
                                    isImagePickDialog = true
                                }
                                .width(120.dp)
                                .height(120.dp)
                                .align(Alignment.CenterHorizontally)) {
                            val (imageUser, imageEdit, spacer) = createRefs()
                            GlideImage(
                                model = imagePath,
                                contentDescription = getString(R.string.app_name),
                                modifier = Modifier
                                    .constrainAs(imageUser) {}
                                    .width(120.dp)
                                    .height(120.dp)) {
                                it.placeholder(R.drawable.ic_launcher_foreground).circleCrop()
                            }
                            Spacer(
                                modifier = Modifier
                                    .width(10.dp)
                                    .constrainAs(spacer) {
                                        bottom.linkTo(imageUser.bottom)
                                        end.linkTo(imageUser.end)
                                    })
                        }
                        MarginVertical(height = 25.dp)
                        MyText(
                            text = resources.getString(R.string.signUpNow),
                            modifier = Modifier.fillMaxWidth(),
                            style = text22Bold().copy(textAlign = TextAlign.Center)
                        )
                        MarginVertical(height = 25.dp)
                        MyText(
                            text = resources.getString(R.string.signUpNowTitle),
                            modifier = Modifier.fillMaxWidth(),
                            style = text14Regular().copy(textAlign = TextAlign.Center)
                        )
                        MarginVertical(height = 25.dp)
                        MyEditTextField(
                            value = name,
                            errorMSg = nameError,
                            keyboardOptions = textKeyBord(),
                            label = stringResource(R.string.fullName),
                        ) {
                            name = it
                        }
                        MyEditTextField(
                            value = email,
                            errorMSg = emailError,
                            keyboardOptions = emailKeyBord(),
                            label = stringResource(R.string.email),
                        ) {
                            email = it
                        }
                        MyEditTextField(
                            value = password,
                            errorMSg = passwordError,
                            keyboardOptions = passwordKeyBord(imeAction = ImeAction.Done),
                            label = stringResource(R.string.password),
                        ) {
                            password = it
                        }
                        ElevatedButton(
                            onClick = {
                                nameError = ""
                                emailError = ""
                                passwordError = ""
                                registerViewModel.register(
                                    name.trim(), email.trim(), password.trim(), imagePath
                                )
                            },
                            modifier = Modifier
                                .padding(top = 40.dp)
                                .align(Alignment.CenterHorizontally)
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            MyText(
                                text = resources.getString(R.string.signUp)
                            )
                        }

                    }
                    ShowLoader(isShowLoading)
                    if (isShowDialog) {
                        ShowMyDialog(
                            yes = { isShowDialog = false },
                            no = { isShowDialog = false },
                            title = resources.getString(R.string.app_name),
                            msg = dialogMSG,
                            isShowDismiss = false
                        )
                    }
                    if (isShowImagePicker) {
                        ImagePickerBottomSheet(
                            list = uri,
                            onImageClick = {
                                imagePath = PathUtil.getPath(this, it).toString()
                            },
                            isShowBottomSheet = isShowImagePicker,
                            onDismissRequest = { isShowImagePicker = false }
                        )
                    }
                    if (isShowDialogSetting) {
                        CustomDialog(
                            title = stringResource(R.string.setting),
                            positiveBtnText = stringResource(R.string.ok),
                            negativeBtnText = stringResource(R.string.cancel),
                            des = stringResource(R.string.setting_des),
                            onPositive = {
                                isShowDialogSetting = false
                                openSetting()
                            },
                            onNegative = {
                                isShowDialogSetting = false
                            },
                            onDismiss = {
                                isShowDialogSetting = false
                            })
                    }
                }
            }
        }
    }

}