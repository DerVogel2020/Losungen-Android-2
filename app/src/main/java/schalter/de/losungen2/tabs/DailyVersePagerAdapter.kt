package schalter.de.losungen2.tabs

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import schalter.de.losungen2.screens.daily.DailyVerseFragment
import java.util.*

private const val dateFormat = "E, dd.MM"

class DailyVersePagerAdapter(fm: FragmentManager) : DatePagerAdapter(fm, dateFormat, Calendar.DAY_OF_MONTH) {

    override fun getItem(position: Int): Fragment = DailyVerseFragment.newInstance(dateList[position])
}