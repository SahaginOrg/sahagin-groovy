package org.sahagin.groovy.runlib.srctreegen.SrcTreeGeneratorTestRes.variousData.input

import org.junit.Test
import geb.junit4.GebTest
import org.sahagin.runlib.external.TestDoc

class TestMain extends GebTest {

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

    @Test
    void 宿泊予約が成功すること() {
        // 予約情報入力ページ
        ReserveInputPage page = to ReserveInputPage
        page.setReserveDate("2015", "5", "30")
        page.reserveTerm = "1"
        page.headCount = "2"
        page.breakfast = "on"
        page.earlyCheckInPlan = true
        page.guestName = "サンプルユーザー"
        page.goNextButton.click()
    }

    @Test
    void 宿泊予約が成功すること2() {
        go("http://example.selenium.jp/reserveApp")
        $("#reserve_year").value("2015")
        $("#reserve_month").value("8")
        $("#reserve_day").value("1")
        $("#guestname").value("サンプルユーザー")
        $("#goto_next").click()
    }
}
