package com.five.yy.vpn.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.five.yy.vpn.R
import com.five.yy.vpn.adapter.ServerAdapter
import com.five.yy.vpn.base.BaseActivity
import com.five.yy.vpn.databinding.ActivityServerBinding
import com.five.yy.vpn.entity.Country
import com.five.yy.vpn.entity.CountryBean
import com.five.yy.vpn.entity.SmartBean
import com.five.yy.vpn.utils.Constant
import com.five.yy.vpn.utils.EntityUtils
import com.five.yy.vpn.utils.InterNetUtil
import com.five.yy.vpn.utils.SPUtils
import com.github.shadowsocks.Core
import com.github.shadowsocks.database.ProfileManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList

class ServiceActivity : BaseActivity<ActivityServerBinding>() {
    private var isConnection = false
    private var serverAdapter: ServerAdapter? = null
    private val serverLayoutManager by lazy {
        LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isConnection = intent.getBooleanExtra("isConnection", false)
        serverAdapter = ServerAdapter()
        binding.allServerRv.layoutManager = serverLayoutManager
        binding.allServerRv.adapter = serverAdapter
        initListener()
        setServerList()

//        loadAd()
    }

    private fun initListener() {
        binding.idBack.setOnClickListener {
            finish()
        }
        serverAdapter?.setOnItemClickListener(object : ServerAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, country: Country) {
                if (country.isChoose == true) {
                    finish()
                } else {
                    if (isConnection) {
                        val alertDialog = AlertDialog.Builder(this@ServiceActivity)
                            .setMessage("If you want to connect to another VPN, you need to disconnect the current connection first. Do you want to disconnect the current connection?")
                            .setPositiveButton("yes") { p0, p1 ->
                                EventBus.getDefault().post(country)
                                saveConnectingCountryBean(country)
                                finish()
                            }
                            .setNegativeButton("no", null)
                            .create()
                        alertDialog.show()
                    } else {
                        EventBus.getDefault().post(country)
                        saveConnectingCountryBean(country)
                        finish()
                    }
                }
            }
        })
        binding.serverSmart.setOnClickListener {
            var countryBean: CountryBean?
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
                    if (isConnection) {
                        val alertDialog = AlertDialog.Builder(this@ServiceActivity)
                            .setMessage("If you want to connect to another VPN, you need to disconnect the current connection first. Do you want to disconnect the current connection?")
                            .setPositiveButton("yes") { p0, p1 ->
                                EventBus.getDefault().post(country)
                                saveConnectingCountryBean(country)
                                finish()
                            }
                            .setNegativeButton("no", null)
                            .create()
                        alertDialog.show()
                    } else {
                        EventBus.getDefault().post(country)
                        saveConnectingCountryBean(country)
                        finish()
                    }
                }
            }

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

    @SuppressLint("NotifyDataSetChanged")
    private fun setServerList() {
        val countryJson: String? = SPUtils.get().getString(Constant.service, "")
        val countryServerList: MutableList<Country> = ArrayList()

        if (countryJson != null) {
            if (countryJson.isNotEmpty()) {
                val type: Type = object : TypeToken<List<CountryBean?>?>() {}.type
                val countryBean: MutableList<CountryBean> =
                    Gson().fromJson(countryJson.toString(), type)
                if (countryBean.isNotEmpty()) {
                    countryBean.forEach {
                        val country: Country = EntityUtils().countryBeanToCountry(it)
                        countryServerList.add(country)
                    }
                }
            }
        }

        val countryString = SPUtils.get().getString(Constant.chooseCountry, "")
        if (countryString != null && countryString.isNotEmpty()) {
            val country = Gson().fromJson(countryString, Country::class.java)
            if (country != null) {
                val profileName = country.name
                if (profileName?.contains("Faster") == true) {
                    binding.serverSmart.setBackgroundResource(R.drawable.disconnect_background)
                    binding.itemChoose.setImageResource(R.mipmap.choose)
                } else {
                    for (item in countryServerList) {
                        if (profileName?.contains(item.name!!) == true) {
                            item.isChoose = true
                        }
                    }
                }
            }
        }

        serverAdapter?.setList(countryServerList)
        serverAdapter?.notifyDataSetChanged()

    }

    private fun saveConnectingCountryBean(event: Country) {
        val countryJson: String? = SPUtils.get().getString(Constant.service, "")
        if (countryJson != null) {
            if (countryJson.isNotEmpty()) {
                val type: Type = object : TypeToken<List<CountryBean?>?>() {}.type
                val countryBean: MutableList<CountryBean> =
                    Gson().fromJson(countryJson.toString(), type)
                if (countryBean.isNotEmpty()) {
                    if (event.name?.contains("Faster Server") == true) {
                        val countryData = CountryBean()
                        countryData.country = event.name!!
                        SPUtils.get()
                            .putString(Constant.connectingCountryBean, Gson().toJson(countryData))
                    } else {
                        countryBean.forEach {
                            if (event.name?.equals(it.country) == true) {
                                SPUtils.get()
                                    .putString(Constant.connectingCountryBean, Gson().toJson(it))
                            }
                        }
                    }
                }
            }
        }
    }

//    override fun onBackPressed() {
//        showAd()
//    }

//    private var adManage = AdManage()
//    private fun loadAd() {
//        val adBean = Constant.AdMap[Constant.adInterstitial_r]
//        if (adBean?.ad == null) {
//            adManage.loadAd(
//                Constant.adInterstitial_r,
//                this
//            )
//        }
//    }
//
//    private fun showAd() {
//        val adBean = Constant.AdMap[Constant.adInterstitial_r]
//        var time: Long = 0
//        if (adBean != null) {
//            time = System.currentTimeMillis() - adBean.saveTime
//        } else {
//            finish()
//            return
//        }
//        if (adBean.ad != null || time < 50 * 60 * 1000) {
//            adManage.showAd(
//                this@ServiceActivity,
//                Constant.adInterstitial_r,
//                adBean,
//                null,
//                object : AdManage.OnShowAdCompleteListener {
//                    override fun onShowAdComplete() {
//                        finish()
//                    }
//
//                    override fun isMax() {
//                        finish()
//                    }
//                })
//
//        } else {
//            finish()
//        }
//    }
}