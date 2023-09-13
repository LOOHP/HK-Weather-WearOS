package com.loohp.hkweatherwarnings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.loohp.hkweatherwarnings.shared.Registry
import com.loohp.hkweatherwarnings.theme.HKWeatherWarningsTheme
import com.loohp.hkweatherwarnings.utils.RemoteActivityUtils
import com.loohp.hkweatherwarnings.utils.StringUtils


class TitleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainElements(this)
        }
    }

}

@Composable
fun MainElements(instance: TitleActivity) {
    HKWeatherWarningsTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimeText(
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp, 0.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OpenHKOAppButton(instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
            LanguageButton(instance)
            Spacer(modifier = Modifier.size(StringUtils.scaledSize(7, instance).dp))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp, 15.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            CreditVersionText(instance)
        }
    }
}

@Composable
fun OpenHKOAppButton(instance: TitleActivity) {
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("myobservatory:"))
            RemoteActivityUtils.intentToPhone(
                instance = instance,
                intent = intent,
                noPhone = {
                    instance.runOnUiThread {
                        Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Unable to connect to phone" else "無法連接到手機", Toast.LENGTH_SHORT).show()
                    }
                },
                failed = {
                    val playIntent = Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=hko.MyObservatory_v1_0"))
                    RemoteActivityUtils.intentToPhone(
                        instance = instance,
                        intent = playIntent,
                        failed = {
                            instance.runOnUiThread {
                                Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Failed to connect to phone" else "連接手機失敗", Toast.LENGTH_SHORT).show()
                            }
                        },
                        success = {
                            instance.runOnUiThread {
                                Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Please check your phone" else "請在手機上繼續", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                success = {
                    instance.runOnUiThread {
                        Toast.makeText(instance, if (Registry.getInstance(instance).language == "en") "Please check your phone" else "請在手機上繼續", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(StringUtils.scaledSize(220, instance), instance).dp)
            .height(StringUtils.scaledSize(StringUtils.scaledSize(45, instance), instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp),
                text = if (Registry.getInstance(instance).language == "en") "Open MyObservatory" else "開啟我的天文台"
            )
        }
    )
}

@Composable
fun LanguageButton(instance: TitleActivity) {
    Button(
        onClick = {
            Registry.getInstance(instance).setLanguage(if (Registry.getInstance(instance).language == "en") "zh" else "en", instance)
            instance.startActivity(Intent(instance, TitleActivity::class.java))
            instance.finish()
        },
        modifier = Modifier
            .width(StringUtils.scaledSize(StringUtils.scaledSize(220, instance), instance).dp)
            .height(StringUtils.scaledSize(StringUtils.scaledSize(45, instance), instance).dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = MaterialTheme.colors.primary
        ),
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = TextUnit(StringUtils.scaledSize(16F, instance), TextUnitType.Sp),
                text = if (Registry.getInstance(instance).language == "en") "中文" else "English"
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreditVersionText(instance: TitleActivity) {
    val packageInfo = instance.packageManager.getPackageInfo(instance.packageName, 0)
    val haptic = LocalHapticFeedback.current
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("https://play.google.com/store/apps/details?id=com.loohp.hkweatherwarnings"))
                    RemoteActivityUtils.intentToPhone(
                        instance = instance,
                        intent = intent,
                        noPhone = {
                            instance.startActivity(intent)
                        },
                        failed = {
                            instance.startActivity(intent)
                        }
                    )
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val intent = Intent(Intent.ACTION_VIEW)
                        .addCategory(Intent.CATEGORY_BROWSABLE)
                        .setData(Uri.parse("https://loohpjames.com"))
                    RemoteActivityUtils.intentToPhone(
                        instance = instance,
                        intent = intent,
                        noPhone = {
                            instance.startActivity(intent)
                        },
                        failed = {
                            instance.startActivity(intent)
                        }
                    )
                }
            ),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        fontSize = TextUnit(StringUtils.scaledSize(1.5F, instance), TextUnitType.Em),
        text = instance.resources.getString(R.string.app_name).plus(" v").plus(packageInfo.versionName).plus(" (").plus(packageInfo.longVersionCode).plus(")\n@LoohpJames")
    )
}