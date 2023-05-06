package com.five.yy.vpn

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.five.yy.vpn.activity.PrivacyPolicyWebView
import com.five.yy.vpn.activity.ConnectStatusResultActivity
import com.five.yy.vpn.activity.ServiceActivity
import com.five.yy.vpn.base.BaseActivity
import com.five.yy.vpn.databinding.ActivityMainBinding
import com.five.yy.vpn.entity.Country
import com.five.yy.vpn.entity.CountryBean
import com.five.yy.vpn.entity.SmartBean
import com.five.yy.vpn.utils.*
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity<ActivityMainBinding>(), ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    private var state = BaseService.State.Idle
    private var isClickConnect = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        initView()
        connection.connect(this, this)
        EventBus.getDefault().register(this)
        onCLickListener()
    }

    private fun onCLickListener() {
        binding.contentLayout.homeConnectLogo.setOnClickListener {
            if (binding.drawerLayout.isOpen || !isClick()) {
                return@setOnClickListener
            } else {
                binding.contentLayout.connectStatus.progress = 0.0
            }
            val intent = Intent(this, ServiceActivity::class.java)
            intent.putExtra("isConnection", state.canStop)
            startActivity(intent)
        }
        binding.contentLayout.homeSettingSrc.setOnClickListener {
            if (!isClick()) {
                return@setOnClickListener
            } else {
                binding.contentLayout.connectStatus.progress = 0.0
            }
            if (binding.drawerLayout.isOpen) {
                binding.drawerLayout.close()
            } else {
                binding.drawerLayout.open()
            }
        }
        binding.contentLayout.homeServerSrc.setOnClickListener {
            if (binding.drawerLayout.isOpen || !isClick()) {
                return@setOnClickListener
            } else {
                binding.contentLayout.connectStatus.progress = 0.0
            }
            //choose service
            val intent = Intent(this, ServiceActivity::class.java)
            intent.putExtra("isConnection", state.canStop)
            startActivity(intent)
        }
        binding.contentLayout.connectStatus.setOnClickListener {
            if (binding.drawerLayout.isOpen || !isClick()) {
                return@setOnClickListener
            } else {
                binding.contentLayout.connectStatus.progress = 0.0
            }
            isClickConnect = true
            if (!ButtonUtils.isFastDoubleClick(R.id.connect_status)) {
                connectVpn()
            }
        }
        binding.settingLayout.contactUs.setOnClickListener {
            if (binding.drawerLayout.isOpen) {
                openSystemMail()
            }
        }

        binding.settingLayout.privacyPolicy.setOnClickListener {
            if (binding.drawerLayout.isOpen) {
                jumpActivity(PrivacyPolicyWebView::class.java)
            }
        }
        binding.settingLayout.shareTv.setOnClickListener {
            if (binding.drawerLayout.isOpen) {
                val intent = Intent()
                intent.action = Intent.ACTION_SEND
                intent.putExtra(Intent.EXTRA_TEXT, Constant.shareUrl)
                intent.type = "text/plain"
                startActivity(intent)
            }
        }
        binding.contentLayout.homeServerSrc.setOnClickListener {
            if (binding.drawerLayout.isOpen || !isClick()) {
                return@setOnClickListener
            } else {
                binding.contentLayout.connectStatus.progress = 0.0
            }
            //choose service
            val intent = Intent(this, ServiceActivity::class.java)
            intent.putExtra("isConnection", state.canStop)
            startActivity(intent)
        }
    }

    private fun openSystemMail() {
        val uri: Uri = Uri.parse("mailto:" + Constant.mail)
        val packageInfos: List<ResolveInfo> =
            packageManager!!.queryIntentActivities(Intent(Intent.ACTION_SENDTO, uri), 0)
        val tempPkgNameList: MutableList<String> = ArrayList()
        val emailIntents: MutableList<Intent> = ArrayList()
        for (info in packageInfos) {
            val pkgName = info.activityInfo.packageName
            if (!tempPkgNameList.contains(pkgName)) {
                tempPkgNameList.add(pkgName)
                val intent: Intent? = packageManager!!.getLaunchIntentForPackage(pkgName)
                if (intent != null) {
                    emailIntents.add(intent)
                }
            }
        }
        if (emailIntents.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            startActivity(intent)
            val chooserIntent =
                Intent.createChooser(intent, "Please select mail application")
            if (chooserIntent != null) {
                startActivity(chooserIntent)
            } else {
                showDialogByActivity("Please set up a Mail account", "OK", true, null)
            }
        } else {
            showDialogByActivity("Please set up a Mail account", "OK", true, null)
        }
    }

    @SuppressLint("SetTextI18n", "ResourceAsColor")
    private fun initView() {
        if (Constant.isShowLead) {
            val customizedDialog = CustomizedDialog(this, "images/main_lead.json", false, true)
            if (!customizedDialog.isShowing) {
                binding.contentLayout.connectStatus.visibility = View.INVISIBLE
                customizedDialog.show()
            }
            customizedDialog.setOnClick {
                Constant.isShowLead = false
                customizedDialog.dismiss()
                binding.contentLayout.connectStatus.visibility = View.VISIBLE
                if (!state.canStop) {
                    if (!ButtonUtils.isFastDoubleClick(R.id.animation_view)) {
                        connectVpn()
                    }
                }

            }
            customizedDialog.setOnCancelListener {
                Constant.isShowLead = false
                binding.contentLayout.connectStatus.visibility = View.VISIBLE
            }
        }
        val countryString = SPUtils.get().getString(Constant.chooseCountry, "")
        if (countryString != null && countryString.isNotEmpty()) {
            val country = Gson().fromJson(countryString, Country::class.java)
            if (country != null) {
                country.src?.let { it1 ->
                    binding.contentLayout.homeConnectLogo.setBackgroundResource(
                        it1
                    )
                }
            }
        }
        binding.contentLayout.connectStatus.setOutGradient(
            false,
            Color.parseColor("#50F7FF"),
            Color.parseColor("#3787E6")
        )
        binding.contentLayout.connectStatus.text = "Disconnected"
    }

    private fun connectVpn() {
        if (InterNetUtil().isShowIR()) {
            showDialogByActivity(
                "Due to the policy reason , this service is not available in your country",
                "confirm", false
            ) { dialog, which -> finish() }

        } else {
            isHasNet()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshUi() {
        var countryBeanJson = ""
        if (state.canStop) {
            if (SPUtils.get()
                    .getString(Constant.connectedCountryBean, "")?.isNotEmpty() == true
            ) {
                countryBeanJson = SPUtils.get()
                    .getString(Constant.connectedCountryBean, "").toString()
                Log.e("mainServiceChoose", "connected")
            }
        }
        if (countryBeanJson?.isEmpty() == true) {
            countryBeanJson = SPUtils.get()
                .getString(Constant.connectingCountryBean, "").toString()
            Log.e("mainServiceChoose", "connecting")

        }

        if (countryBeanJson != null) {
            val countryBean = Gson().fromJson(countryBeanJson, CountryBean::class.java)
            if (countryBean != null) {
                val country = EntityUtils().countryBeanToCountry(countryBean)
                country.src?.let { it1 ->
                    binding.contentLayout.homeConnectLogo.setBackgroundResource(
                        it1
                    )
                }
                Log.e("mainServiceChoose", country?.name + "-" + country?.city)
            }
        }

    }

    private fun toggle() = if (state.canStop) showConnect() else connect.launch(null)
    private fun isHasNet() {
        if (InterNetUtil().isNetConnection(this)) {
            toggle()
        } else {
            showDialogByActivity("Please check your network", "OK", true, null)
        }
    }

    private val connect = registerForActivityResult(StartService()) {
        if (!it) {
            showConnect()
        }
    }
    private var countryBean: CountryBean? = null
    private fun connectAnnotation() {
        ProfileManager.clear()
//        var countryBean: CountryBean? = null
        val countryBeanJson = if (state.canStop) SPUtils.get()
            .getString(Constant.connectedCountryBean, "") else SPUtils.get()
            .getString(Constant.connectingCountryBean, "")
        if (countryBeanJson != null) {
            if (countryBeanJson.isNotEmpty()) {
                countryBean = Gson().fromJson(countryBeanJson, CountryBean::class.java)
            }
        }
        if (countryBean == null) {
            runBlocking {
                val smartList = getFastSmart()
                if (smartList.isNotEmpty()) {
                    val fast = if (smartList.size >= 3) {
                        Random().nextInt(3)
                    } else {
                        Random().nextInt(smartList.size)
                    }
                    countryBean = smartList[fast].smart
                    countryBean?.country = "Faster Server"
                    val country = EntityUtils().countryBeanToCountry(countryBean!!)
                    country.src = R.mipmap.fast
                    val profile = EntityUtils().countryToProfile(country)
                    val profileNew = ProfileManager.createProfile(profile)
                    Core.switchProfile(profileNew.id)
                    SPUtils.get()
                        .putString(Constant.connectingCountryBean, Gson().toJson(countryBean))
                }
            }
        } else {
            val country = countryBean?.let { EntityUtils().countryBeanToCountry(it) }
            val profile = country?.let { EntityUtils().countryToProfile(it) }
            val profileNew = profile?.let { ProfileManager.createProfile(it) }
            profileNew?.id?.let { Core.switchProfile(it) }
            SPUtils.get().putString(Constant.connectingCountryBean, Gson().toJson(countryBean))
        }
    }

    private suspend fun getFastSmart(): MutableList<SmartBean> {
        val smartJson = SPUtils.get().getString(Constant.smart, "")
        val serviceJson = SPUtils.get().getString(Constant.service, "")
        var serviceList: MutableList<CountryBean> = mutableListOf()
        val smartBeanList: MutableList<SmartBean> = mutableListOf()
        if (serviceJson?.isNotEmpty() == true) {
            val serviceType: Type = object : TypeToken<List<CountryBean?>?>() {}.type
            serviceList = Gson().fromJson(serviceJson, serviceType)
        }
        if (smartJson?.isNotEmpty() == true) {
            val type: Type = object : TypeToken<List<String?>?>() {}.type
            val smartList: MutableList<String> = Gson().fromJson(smartJson, type)
            if (smartList.isNotEmpty() && serviceList.isNotEmpty()) {
                for (item in smartList) {
                    for (service in serviceList) {
                        if (item == service.city) {
                            smartBeanList.add(
                                SmartBean(
                                    service,
                                    InterNetUtil().delayTest(service.ip, 1)
                                )
                            )
                        }
                    }

                }
            }
        }
        return smartBeanList
    }

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeConnectionStatus(state)
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        changeConnectionStatus(
            try {
                BaseService.State.values()[service.state]
            } catch (_: RemoteException) {
                BaseService.State.Idle
            }
        )
    }

    private val connection = ShadowsocksConnection(true)
    override fun onBinderDied() {
        connection.disconnect(this)
        connection.connect(this, this)
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    private fun changeConnectionStatus(status: BaseService.State) {
        this.state = status
        isClickConnect = false
        when (status) {
            BaseService.State.Idle -> {
                SPUtils.get().putBoolean(Constant.isConnectStatus, false)
//                binding.contentLayout.connectStatus.setBackgroundResource(R.drawable.connect_button)
                binding.contentLayout.theConnectionTimeTv.stop()
                binding.contentLayout.theConnectionTimeTv.text = "00:00:00"
                binding.contentLayout.connectStatus.text = "Disconnected"
                binding.contentLayout.connectStatus.progress = 0.0
                Toast.makeText(this, "please try again", Toast.LENGTH_LONG).show()
            }
            BaseService.State.Connected -> {
                SPUtils.get().putBoolean(Constant.isConnectStatus, true)
                if (countryBean != null) {
                    SPUtils.get()
                        .putString(Constant.connectedCountryBean, Gson().toJson(countryBean))
                    SPUtils.get().putString(
                        Constant.chooseCountry,
                        Gson().toJson(EntityUtils().countryBeanToCountry(countryBean!!))
                    )
                }
//                binding.contentLayout.connectStatus.setBackgroundResource(R.drawable.disconnect_button)
                binding.contentLayout.connectStatus.text = "Connected"
                binding.contentLayout.connectStatus.progress = 100.0
                binding.contentLayout.theConnectionTimeTv.setOnChronometerTickListener {
                    val time = SystemClock.elapsedRealtime() - it.base
                    val date = Date(time)
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    binding.contentLayout.theConnectionTimeTv.text = sdf.format(date)
                }
                val connectTime = SPUtils.get().getLong(Constant.connectTime, 0)
                if (connectTime > 0) {
                    binding.contentLayout.theConnectionTimeTv.base = connectTime
                } else {
                    binding.contentLayout.theConnectionTimeTv.base = SystemClock.elapsedRealtime()
                }
                if (SystemClock.elapsedRealtime() - (binding.contentLayout.theConnectionTimeTv.base) < 20 && SPUtils.get()
                        .getBoolean(Constant.isShowResultKey, false)
                ) {
                    lifecycleScope.launch(Dispatchers.Main.immediate) {
                        delay(300L)
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            var country: Country? = null
                            if (countryBean != null) {
                                country = EntityUtils().countryBeanToCountry(countryBean!!)
                            }
                            val srcInt = if (country != null) country!!.src else R.mipmap.fast
                            val intent =
                                Intent(this@MainActivity, ConnectStatusResultActivity::class.java)
                            intent.putExtra("base", binding.contentLayout.theConnectionTimeTv.base)
                            intent.putExtra("srcInt", srcInt)
                            startActivity(intent)
                            SPUtils.get().putBoolean(Constant.isShowResultKey, false)
                            refreshUi()
                        }
                    }
                }
                binding.contentLayout.theConnectionTimeTv.start()
            }
            BaseService.State.Stopped -> {
//                binding.contentLayout.connectStatus.setBackgroundResource(R.drawable.connect_button)
                binding.contentLayout.connectStatus.text = "Disconnected"
                binding.contentLayout.connectStatus.progress = 0.0
                binding.contentLayout.theConnectionTimeTv.stop()
                binding.contentLayout.theConnectionTimeTv.text = "00:00:00"
                SPUtils.get().putLong(Constant.connectTime, 0L)
                if (SPUtils.get()
                        .getBoolean(Constant.isShowResultKey, false) && Constant.text != "00:00:00"
                ) {
                    SPUtils.get().putBoolean(Constant.isConnectStatus, false)
                    var country: Country? = null
                    if (countryBean != null) {
                        country = EntityUtils().countryBeanToCountry(countryBean!!)
                    }
                    val srcInt = if (country != null) country!!.src else R.mipmap.fast
                    lifecycleScope.launch(Dispatchers.Main.immediate) {
                        delay(300L)
                        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            val intent =
                                Intent(this@MainActivity, ConnectStatusResultActivity::class.java)
                            intent.putExtra("text", Constant.text)
                            intent.putExtra("isStop", true)
                            intent.putExtra("srcInt", srcInt)
                            startActivity(intent)
                            SPUtils.get().putBoolean(Constant.isShowResultKey, false)
                            refreshUi()
                            val countryBeanJson =
                                SPUtils.get().getString(Constant.connectingCountryBean, "")
                            if (countryBeanJson != null && countryBeanJson.isNotEmpty()) {
                                val countryBeanConnecting =
                                    Gson().fromJson(countryBeanJson, CountryBean::class.java)
                                if (countryBeanConnecting != null) {
                                    SPUtils.get().putString(
                                        Constant.chooseCountry,
                                        Gson().toJson(
                                            EntityUtils().countryBeanToCountry(
                                                countryBeanConnecting
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                }
            }
            else -> {
                SPUtils.get().putBoolean(Constant.isConnectStatus, false)
//                binding.contentLayout.connectStatus.setBackgroundResource(R.drawable.connect_button)
                binding.contentLayout.connectStatus.text = "Disconnected"
                binding.contentLayout.connectStatus.progress = 0.0
                binding.contentLayout.theConnectionTimeTv.base = SystemClock.elapsedRealtime()
                binding.contentLayout.theConnectionTimeTv.stop()
                binding.contentLayout.theConnectionTimeTv.text = "00:00:00"
                SPUtils.get().putLong(Constant.connectTime, 0L)
            }
        }
    }

    private var connectionJob: Job? = null
    private var time: Int = 0
    private var interAdIsShow = false

    private fun showConnect() {
        time = 0
        if (state.canStop) {
            Constant.text = binding.contentLayout.theConnectionTimeTv.text as String
        } else {
            binding.contentLayout.connectStatus.text = "Connecting"
        }
        SPUtils.get().putBoolean(Constant.isShowResultKey, true)

        connectionJob = lifecycleScope.launch {
            flow {
                (0 until 10).forEach {
                    delay(1000)
                    time += 1
                    emit(it)
                }
            }.onStart {
                //start
                if (state.canStop) {
                    binding.contentLayout.connectStatus.text = "Disconnecting"
                } else {
                    binding.contentLayout.connectStatus.text = "Connecting"
                }
                connectAnnotation()
            }.onCompletion {
                //finish
                if (binding.contentLayout.connectStatus.progress > 0) {
                    if (state.canStop) {
                        Core.stopService()
                    } else {
                        Core.startService()
                    }
                }

            }.collect {
                //process
//                if (time == 1) {
//                    loadInterAd(customizedDialog!!)
//                }

                if (binding.contentLayout.connectStatus.progress == 0.0 && time > 1) {
                    connectionJob?.cancel()
                    return@collect
                }
                binding.contentLayout.connectStatus.progress = (time * 100 / 10).toDouble()
//                if (customizedDialog?.isShowing == false) {
//                    connectionJob?.cancel()
//                    return@collect
//                }
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: Country?) {
        connectVpn()
    }

    override fun onDestroy() {
        super.onDestroy()
        SPUtils.get().putLong(Constant.connectTime, binding.contentLayout.theConnectionTimeTv.base)
        SPUtils.get().putBoolean(Constant.isShowResultKey, false)
        EventBus.getDefault().unregister(this)
        countryBean = null
    }


    private fun isClick(): Boolean {
        if (binding.contentLayout.connectStatus.progress > 0 && binding.contentLayout.connectStatus.progress < 100 && !state.canStop) {
            return false
        } else {
            if (isClickConnect) {
                return false
            }
        }
        return true
    }

    override fun onRestart() {
        super.onRestart()
        isClickConnect = false
    }

    override fun onBackPressed() {
        if (isClick()) {
            binding.contentLayout.connectStatus.progress = 0.0
            finish()
        }
    }
}