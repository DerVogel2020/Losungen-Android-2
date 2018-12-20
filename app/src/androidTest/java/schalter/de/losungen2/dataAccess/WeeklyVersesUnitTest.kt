package schalter.de.losungen2.dataAccess

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import schalter.de.losungen2.TestUtils
import java.io.IOException
import java.util.*

@RunWith(AndroidJUnit4::class)
class WeeklyVersesUnitTest {
    private lateinit var weeklyVerseDao: WeeklyVersesDao
    private lateinit var db: VersesDatabase

    private var date = Calendar.getInstance().time
    private var weeklyVerse = WeeklyVerse(
            date = date,
            verseBible = "verse bible",
            verseText = "verse text",
            language = Language.DE
    )

    @Before
    fun createDb() {
        val context: Context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
                context, VersesDatabase::class.java).build()
        weeklyVerseDao = db.weeklyVerseDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeVerseAndRead() {
        weeklyVerseDao.insertWeeklyVerse(weeklyVerse)
        val verseFromDatabase: WeeklyVerse = weeklyVerseDao.findWeeklyVerseByDate(weeklyVerse.date)
        assertThat(verseFromDatabase.verseBible, equalTo(weeklyVerse.verseBible))
        assertThat(verseFromDatabase.verseText, equalTo(weeklyVerse.verseText))
        assertThat(verseFromDatabase.date, equalTo(weeklyVerse.date))
    }

    @Test
    @Throws(Exception::class)
    fun writeMultipleVersesAndRead() {
        weeklyVerseDao.insertWeeklyVerse(weeklyVerse)

        val weeklyVerseNextWeek = weeklyVerse.copy()
        weeklyVerseNextWeek.verseText = "2"
        weeklyVerseNextWeek.date = TestUtils.addDaysToDate(weeklyVerse.date, 7)
        weeklyVerseDao.insertWeeklyVerse(weeklyVerseNextWeek)

        var verseFromDatabase: WeeklyVerse = weeklyVerseDao.findWeeklyVerseByDate(weeklyVerse.date)
        assertThat(verseFromDatabase.verseText, equalTo(weeklyVerse.verseText))
        assertThat(verseFromDatabase.verseBible, equalTo(weeklyVerse.verseBible))
        assertThat(verseFromDatabase.date, equalTo(weeklyVerse.date))
        assertThat(verseFromDatabase.isFavourite, equalTo(weeklyVerse.isFavourite))
        assertThat(verseFromDatabase.language, equalTo(weeklyVerse.language))
        assertThat(verseFromDatabase.notes, equalTo(weeklyVerse.notes))

        verseFromDatabase = weeklyVerseDao.findWeeklyVerseByDate(weeklyVerseNextWeek.date)
        assertThat(verseFromDatabase.verseText, equalTo(weeklyVerseNextWeek.verseText))
        assertThat(verseFromDatabase.date, equalTo(weeklyVerseNextWeek.date))
    }

    @Test
    @Throws(Exception::class)
    fun updateLanguage() {
        weeklyVerse.notes = "notes"
        weeklyVerseDao.insertWeeklyVerse(weeklyVerse)

        val weeklyVerseEnglish = weeklyVerse.copy()
        weeklyVerseEnglish.verseText = "english verse"
        weeklyVerseEnglish.verseBible = "english bible"
        weeklyVerseEnglish.language = Language.EN
        weeklyVerseEnglish.notes = "notes english" // should not update

        weeklyVerseDao.updateLanguage(weeklyVerseEnglish)

        val verseFromDatabase: WeeklyVerse = weeklyVerseDao.findWeeklyVerseByDate(weeklyVerseEnglish.date)
        assertThat(verseFromDatabase.verseBible, equalTo(weeklyVerseEnglish.verseBible))
        assertThat(verseFromDatabase.verseText, equalTo(weeklyVerseEnglish.verseText))
        assertThat(verseFromDatabase.language, equalTo(weeklyVerseEnglish.language))
        assertThat(verseFromDatabase.notes, equalTo(weeklyVerse.notes))
    }

    @Test
    fun dateConverting() {
        weeklyVerseDao.insertWeeklyVerse(weeklyVerse)

        val calendar = Calendar.getInstance()
        calendar.time = weeklyVerse.date
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
        } else {
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        // find verse
        var verseFromDatabase: WeeklyVerse = weeklyVerseDao.findWeeklyVerseByDate(calendar.time)
        assertThat(verseFromDatabase.date, equalTo(weeklyVerse.date))

        // update language
        val weeklyVerseOtherDate = weeklyVerse.copy()
        weeklyVerseOtherDate.date = calendar.time
        weeklyVerseOtherDate.verseBible = "other date"
        weeklyVerseDao.updateLanguage(weeklyVerseOtherDate)
        verseFromDatabase = weeklyVerseDao.findWeeklyVerseByDate(weeklyVerse.date)
        assertThat(verseFromDatabase.date, equalTo(weeklyVerse.date))
        assertThat(verseFromDatabase.verseBible, equalTo(weeklyVerseOtherDate.verseBible))
    }
}