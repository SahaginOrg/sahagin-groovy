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
        reserveYear (wait: true) { $(name: "reserve_y") }
        reserveMonth { $(name: "reserve_m") }
        reserveDay { $(name: "reserve_d") }
        reserveTerm { $(name: "reserve_t") }
        headCount { $(name: "hc") }
        breakfast { $(name: "bf") }
        earlyCheckInPlan { $(name: "plan_a") }
        sightseeingPlan { $(name: "plan_b") }
        guestName { $(name: "gname") }
        goNextButton (wait: true){ $("#goto_next") }
    }
    static contentTestDoc = {
        reserveYear { "宿泊年" }
        reserveMonth { "宿泊月" }
        reserveDay { "宿泊日" }
        reserveTerm { "宿泊日数" }
        headCount { "人数" }
        breakfast { "朝食" }
        earlyCheckInPlan { "早めにチェックインプラン" }
        sightseeingPlan { "観光プラン" }
        guestName { "名前" }
        goNextButton { "ボタン「次へ」" }
    }

    @TestDoc(value = "宿泊日に「{year}/{month}/{day}」をセット", capture = CaptureStyle.STEP_IN)
    void setReserveDate(year, month, day) {
        reserveYear = year
        reserveMonth = month
        reserveDay = day
    }
}
