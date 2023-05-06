package com.five.yy.vpn.activity

import android.os.Bundle
import android.os.CountDownTimer
import com.five.yy.vpn.MainActivity
import com.five.yy.vpn.base.BaseActivity
import com.five.yy.vpn.databinding.ActivityFlashBinding
import com.five.yy.vpn.utils.Constant
import com.five.yy.vpn.utils.EntityUtils
import com.five.yy.vpn.utils.InterNetUtil
import com.five.yy.vpn.utils.SPUtils

private const val COUNTER_TIME = 2L
class LoadingActivity : BaseActivity<ActivityFlashBinding>(){
    private var isShowAd = false
    private var countDownTimer: CountDownTimer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        countTimer()
        InterNetUtil().getIpByServer(this)
        setSmartAndServerDate()
    }

    private fun countTimer() {
        countDownTimer = object : CountDownTimer(COUNTER_TIME * 1000, 1000L) {
            override fun onTick(p0: Long) {
                val process = 100 - (p0 * 100 / COUNTER_TIME / 1000)
                binding.progressBar.setProgress(process.toInt())
//                if (process >= 20) {
//                    if (!isShowAd) {
//                        showOpenAd()
//                    }
//                }
            }

            override fun onFinish() {
                jumpActivityFinish(MainActivity::class.java)
            }

        }
        (countDownTimer as CountDownTimer).start()
    }

    override fun onStart() {
        super.onStart()
//        loadOpenAd()
    }

    override fun onRestart() {
        super.onRestart()

        if (countDownTimer != null) {
            countDownTimer?.cancel()
            countTimer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun setSmartAndServerDate() {
        if (SPUtils.get().getString(Constant.smart, "")?.isEmpty() == true) {
            val smartJson = EntityUtils().obtainNativeJsonData(this, "city.json")
            SPUtils.get().putString(Constant.smart, smartJson.toString())
        }
        if (SPUtils.get().getString(Constant.service, "")?.isEmpty() == true) {
            val serviceJson = EntityUtils().obtainNativeJsonData(this, "service.json")
            SPUtils.get().putString(Constant.service, serviceJson.toString())
        }
    }

//    private fun showOpenAd() {
//        val adBean = Constant.AdMap[Constant.adOpen]
//        val adManage = AdManage()
//        var time: Long = 0
//        if (adBean != null) {
//            time = System.currentTimeMillis() - adBean.saveTime
//        }
//        if (adBean?.ad != null && time < 50 * 60 * 1000) {
//            countDownTimer?.cancel()
//            isShowAd = true
//            adManage.showAd(
//                this@FlashActivity,
//                Constant.adOpen,
//                adBean,
//                null,
//                object : AdManage.OnShowAdCompleteListener {
//                    override fun onShowAdComplete() {
//                        jumpActivityFinish(MainActivity::class.java)
//                    }
//
//                    override fun isMax() {
//                        jumpActivityFinish(MainActivity::class.java)
//                    }
//
//                })
//        } else {
//            adManage.loadAd(Constant.adOpen, this, object : AdManage.OnLoadAdCompleteListener {
//                override fun onLoadAdComplete(ad: AdBean?) {
//                }
//
//                override fun isMax() {
//                    jumpActivityFinish(MainActivity::class.java)
//                }
//
//            })
//        }
//    }

//    private fun loadOpenAd() {
//        isShowAd = false
//        var adBean = Constant.AdMap[Constant.adOpen]
//        val adManage = AdManage()
//        if (adBean?.ad == null) {
//            adManage.loadAd(Constant.adOpen, this, object : AdManage.OnLoadAdCompleteListener {
//                override fun onLoadAdComplete(ad: AdBean?) {
//                }
//
//                override fun isMax() {
//                }
//
//            })
//        }
//
//    }

    override fun onBackPressed() {
    }
}