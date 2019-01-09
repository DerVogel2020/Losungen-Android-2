package schalter.de.losungen2.dataAccess

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import schalter.de.losungen2.TestUtils
import schalter.de.losungen2.dataAccess.daos.MonthlyVersesDao
import schalter.de.losungen2.dataAccess.entities.MonthlyVerse
import java.io.IOException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private fun <T> LiveData<T>.blockingObserve(): T? {
    var value: T? = null
    val latch = CountDownLatch(1)

    observeForever { t ->
        value = t
        latch.countDown()
    }

    latch.await(2, TimeUnit.SECONDS)
    return value
}

@RunWith(AndroidJUnit4::class)
class MonthlyVersesUnitTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var monthlyVersesDao: MonthlyVersesDao
    private lateinit var db: VersesDatabase

    private var date = Calendar.getInstance().time
    private var monthlyVerse = MonthlyVerse(
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
        monthlyVersesDao = db.monthlyVerseDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeVerseAndRead() {
        monthlyVersesDao.insertMonthlyVerse(monthlyVerse)
        val verseFromDatabase: MonthlyVerse = monthlyVersesDao.findMonthlyVerseByDate(monthlyVerse.date).blockingObserve()!!
        assertThat(verseFromDatabase.verseBible, equalTo(monthlyVerse.verseBible))
        assertThat(verseFromDatabase.verseText, equalTo(monthlyVerse.verseText))
        assertThat(verseFromDatabase.date, equalTo(monthlyVerse.date))
    }

    @Test
    @Throws(Exception::class)
    fun writeMultipleVersesAndRead() {
        monthlyVersesDao.insertMonthlyVerse(monthlyVerse)

        val monthlyVerseNextMonth = monthlyVerse.copy()
        monthlyVerseNextMonth.verseText = "2"
        monthlyVerseNextMonth.date = TestUtils.addMonthsToDate(monthlyVerse.date, 1)
        monthlyVersesDao.insertMonthlyVerse(monthlyVerseNextMonth)

        var verseFromDatabase: MonthlyVerse = monthlyVersesDao.findMonthlyVerseByDate(monthlyVerse.date).blockingObserve()!!
        assertThat(verseFromDatabase.verseText, equalTo(monthlyVerse.verseText))
        assertThat(verseFromDatabase.verseBible, equalTo(monthlyVerse.verseBible))
        assertThat(verseFromDatabase.date, equalTo(monthlyVerse.date))
        assertThat(verseFromDatabase.isFavourite, equalTo(monthlyVerse.isFavourite))
        assertThat(verseFromDatabase.language, equalTo(monthlyVerse.language))
        assertThat(verseFromDatabase.notes, equalTo(monthlyVerse.notes))

        verseFromDatabase = monthlyVersesDao.findMonthlyVerseByDate(monthlyVerseNextMonth.date).blockingObserve()!!
        assertThat(verseFromDatabase.verseText, equalTo(monthlyVerseNextMonth.verseText))
        assertThat(verseFromDatabase.date, equalTo(monthlyVerseNextMonth.date))
    }

    @Test
    @Throws(Exception::class)
    fun updateLanguage() {
        monthlyVerse.notes = "notes"
        monthlyVersesDao.insertMonthlyVerse(monthlyVerse)

        val monthlyVerseEnglish = monthlyVerse.copy()
        monthlyVerseEnglish.verseText = "english verse"
        monthlyVerseEnglish.verseBible = "english bible"
        monthlyVerseEnglish.language = Language.EN
        monthlyVerseEnglish.notes = "notes english" // should not update

        monthlyVersesDao.updateLanguage(monthlyVerseEnglish)

        val verseFromDatabase: MonthlyVerse = monthlyVersesDao.findMonthlyVerseByDate(monthlyVerseEnglish.date).blockingObserve()!!
        assertThat(verseFromDatabase.verseBible, equalTo(monthlyVerseEnglish.verseBible))
        assertThat(verseFromDatabase.verseText, equalTo(monthlyVerseEnglish.verseText))
        assertThat(verseFromDatabase.language, equalTo(monthlyVerseEnglish.language))
        assertThat(verseFromDatabase.notes, equalTo(monthlyVerse.notes))
    }

    @Test
    fun dateConverting() {
        monthlyVersesDao.insertMonthlyVerse(monthlyVerse)

        val calendar = Calendar.getInstance()
        calendar.time = monthlyVerse.date
        if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
            calendar.set(Calendar.DAY_OF_MONTH, 2)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }

        // find verse
        var verseFromDatabase: MonthlyVerse = monthlyVersesDao.findMonthlyVerseByDate(calendar.time).blockingObserve()!!
        assertThat(verseFromDatabase.date, equalTo(monthlyVerse.date))

        // update language
        val monthlyVerseOtherDate = monthlyVerse.copy()
        monthlyVerseOtherDate.date = calendar.time
        monthlyVerseOtherDate.verseBible = "other date"
        monthlyVersesDao.updateLanguage(monthlyVerseOtherDate)
        verseFromDatabase = monthlyVersesDao.findMonthlyVerseByDate(monthlyVerse.date).blockingObserve()!!
        assertThat(verseFromDatabase.date, equalTo(monthlyVerse.date))
        assertThat(verseFromDatabase.verseBible, equalTo(monthlyVerseOtherDate.verseBible))
    }
}