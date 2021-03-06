package org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorTestRes.variousData.input

import java.util.Calendar

import geb.spock.GebSpec
import org.sahagin.runlib.external.TestDoc
import org.junit.Test

class TestMainSpec extends GebSpec {

    @TestDoc("次の土曜日")
    private static Calendar nextSaturday() {
        Calendar calendar = Calendar.instance
        int weekday = calendar.get(Calendar.DAY_OF_WEEK)
        if (weekday == Calendar.SATURDAY) {
            calendar.add(Calendar.DATE, 7)
        } else {
            calendar.add(Calendar.DATE, Calendar.SATURDAY - weekday)
        }
        return calendar
    }

    @TestDoc("テスト1")
    def "宿泊予約が成功すること"() {
        setup: "予約情報入力ページに遷移"
        to ReserveInputPage
        when: "予約情報を入力"
        setReserveDate("2015", "5", "30")
        reserveTerm = "1"
        headCount = "2"
        breakfast = "on"
        earlyCheckInPlan = true
        guestName = "サンプルユーザー"
        then:
        assert guestName == "サンプルユーザ"
        guestName == "サンプルユーザ"
        when: "次のページへ"
        goNextButton.click()
        then: "表示された予約情報をチェック"
    }

    @TestDoc("テスト2")
    def "宿泊予約が成功すること2"() {
        setup: "予約情報入力ページに遷移"
        go("http://example.selenium.jp/reserveApp")
        when: "予約情報を入力"
        $("#reserve_year").value("2015")
        $("#reserve_month").value("8")
        $("#reserve_day").value("1")
        $("#guestname").value("サンプルユーザー")
        and: "次のページへ"
        $("#goto_next").click()
        then: "表示された予約情報をチェック"
    }
}