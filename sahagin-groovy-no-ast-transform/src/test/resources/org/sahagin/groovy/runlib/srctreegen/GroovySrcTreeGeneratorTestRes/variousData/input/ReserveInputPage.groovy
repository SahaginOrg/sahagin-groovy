package org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorTestRes.variousData.input

import geb.Page
import org.sahagin.runlib.external.PageDoc
import org.sahagin.runlib.external.TestDoc
import org.sahagin.runlib.external.CaptureStyle

@PageDoc("予約情報入力ページ")
class ReserveInputPage extends Page {
    static url = "http://example.selenium.jp/reserveApp"
    static at = { title == "予約情報入力"}
    static content = {
        reserveYear (testDoc: "宿泊年", wait: true) { $(name: "reserve_y") }
        reserveMonth (testDoc: "宿泊月") { $(name: "reserve_m") }
        reserveDay (testDoc: "宿泊日"){ $(name: "reserve_d") }
        reserveTerm (testDoc: "宿泊日数"){ $(name: "reserve_t") }
        headCount (testDoc: "人数") { $(name: "hc") }
        breakfast (testDoc: "朝食"){ $(name: "bf") }
        earlyCheckInPlan (testDoc: "早めにチェックインプラン"){ $(name: "plan_a") }
        sightseeingPlan (testDoc: "観光プラン"){ $(name: "plan_b") }
        guestName (testDoc: "名前"){ $(name: "gname") }
        goNextButton (wait: true, testDoc: "ボタン「次へ」"){ $("#goto_next") }
    }

    @TestDoc(value = "宿泊日に「{year}/{month}/{day}」をセット", capture = CaptureStyle.STEP_IN)
    void setReserveDate(year, month, day) {
        reserveYear = year
        reserveMonth = month
        reserveDay = day
    }
}
