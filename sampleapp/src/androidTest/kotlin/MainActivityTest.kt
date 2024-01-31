import android.Manifest
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.sonova.android.permissionrequester.sample.MainActivity
import com.sonova.android.permissionrequester.sample.R
import com.sonova.android.permissionrequester.test.PermissionTestConfiguration
import com.sonova.android.permissionrequester.test.mockkPermissions
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    @get:Rule
    val composableRule = createEmptyComposeRule()

    @Test
    fun whenAskingForPermissionsThenItShouldShowPermissionDialog() {
        launchActivity(
            userGranted = true,
            shouldShowRationale = false
        )

        assertPermissionDeniedTextExists()
        clickOnButtonWithRationale()
        assertPermissionGrantedTextExists()
    }

    @Test
    fun whenAskingForPermissionsThenItShouldShowRationaleDialogAfterDenying() {
        launchActivity(
            userGranted = false,
            shouldShowRationale = true
        )

        assertPermissionDeniedTextExists()
        clickOnButtonWithRationale()
        onView(withText(R.string.permission_rationale_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        assertPermissionDeniedTextExists()
    }


    @Test
    fun whenAskingForPermissionsThenItShouldShowSettingsDialogAfterDenying() {
        launchActivity(
            userGranted = false,
            shouldShowRationale = false
        )

        assertPermissionDeniedTextExists()
        clickOnButtonWithRationale()
        onView(withText(R.string.permission_settings_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        assertPermissionDeniedTextExists()
    }


    @Test
    fun whenAskingForPermissionWithoutRationaleThenNeverShowRationale() {
        launchActivity(
            userGranted = false,
            shouldShowRationale = true
        )

        assertPermissionDeniedTextExists()

        clickOnButtonWithoutRationale()

        onView(withText(R.string.permission_settings_title))
            .inRoot(isDialog())
            .check(doesNotExist())

        assertPermissionDeniedTextExists()
    }


    @Test
    fun whenUserGrantsPermissionsThenDoNotShowRationales() {
        launchActivity(
            initiallyGranted = true,
            userGranted = false,
            shouldShowRationale = true
        )

        assertPermissionGrantedTextExists()
    }

    private fun clickOnButtonWithRationale() {
        composableRule.onNodeWithTag("WithRationale")
            .performClick()
    }

    private fun clickOnButtonWithoutRationale() {
        composableRule.onNodeWithTag("WithoutRationale")
            .performClick()
    }

    private fun assertPermissionGrantedTextExists() {
        composableRule.onNodeWithText("Location permission granted")
            .assertExists()
    }

    private fun assertPermissionDeniedTextExists() {
        composableRule.onNodeWithText("Location permission denied")
            .assertExists()
    }

    private fun launchActivity(
        initiallyGranted: Boolean = false,
        shouldShowRationale: Boolean = false,
        userGranted: Boolean = false
    ) {
        val permissions = listOf(
            PermissionTestConfiguration(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                initiallyGranted = initiallyGranted,
                userGranted = userGranted,
                shouldShowRationale = shouldShowRationale
            ),
            PermissionTestConfiguration(
                Manifest.permission.ACCESS_FINE_LOCATION,
                initiallyGranted = initiallyGranted,
                userGranted = userGranted,
                shouldShowRationale = shouldShowRationale
            )
        )

        mockkPermissions(permissions)

        ActivityScenario.launch(MainActivity::class.java)
    }

}