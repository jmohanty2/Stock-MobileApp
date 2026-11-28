
package com.joelwhissel.personalstocktracker.ui.details;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.textfield.TextInputLayout;

import com.joelwhissel.personalstocktracker.R;
import com.joelwhissel.personalstocktracker.ServiceLocator;
import com.joelwhissel.personalstocktracker.data.repository.fake.FakeAlertRepository;
import com.joelwhissel.personalstocktracker.data.repository.fake.FakeSettingsRepository;
import com.joelwhissel.personalstocktracker.data.repository.fake.FakeStockRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class StockDetailsActivityTest {

    private ActivityScenario<StockDetailsActivity> scenario;

    @Before
    public void setUp() {

        Context context =
                ApplicationProvider.getApplicationContext();

        // Prevent permission dialog from blocking Espresso test
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .grantRuntimePermission(
                            context.getPackageName(),
                            Manifest.permission.POST_NOTIFICATIONS
                    );
        }

        // Fresh fake repositories for every test.
        FakeAlertRepository alerts =
                new FakeAlertRepository();

        FakeStockRepository stocks =
                new FakeStockRepository(alerts);

        stocks.seedSampleData();

        ServiceLocator.setRepositories(
                stocks,
                alerts,
                new FakeSettingsRepository()
        );

        // Open the Details screen directly for AAPL.
        Intent intent = new Intent(
                context,
                StockDetailsActivity.class
        );

        intent.putExtra(
                StockDetailsActivity.EXTRA_TICKER,
                "AAPL"
        );

        scenario = ActivityScenario.launch(intent);

        InstrumentationRegistry.getInstrumentation()
                .waitForIdleSync();

        assertAlertCount(0);
    }

    @After
    public void tearDown() {

        if (scenario != null) {
            scenario.close();
        }

        ServiceLocator.reset();
    }

    @Test
    public void emptyValue_showsInvalidValue() {

        submitAlert(
                R.id.btn_alert_percent,
                R.id.radio_alert_up,
                ""
        );

        assertInvalidValue();
    }

    @Test
    public void nonNumericValue_showsInvalidValue() {

        submitAlert(
                R.id.btn_alert_percent,
                R.id.radio_alert_up,
                "abc"
        );

        assertInvalidValue();
    }

    @Test
    public void negativeValue_showsInvalidValue() {

        submitAlert(
                R.id.btn_alert_percent,
                R.id.radio_alert_up,
                "-5"
        );

        assertInvalidValue();
    }

    @Test
    public void percentageFallOver100_showsInvalidValue() {

        submitAlert(
                R.id.btn_alert_percent,
                R.id.radio_alert_down,
                "120"
        );

        assertInvalidValue();
    }

    @Test
    public void priceRiseBelowCurrentPrice_showsInvalidValue() {

        submitAlert(
                R.id.btn_alert_price,
                R.id.radio_alert_up,
                "1"
        );

        assertInvalidValue();
    }

    @Test
    public void priceFallAboveCurrentPrice_showsInvalidValue() {

        submitAlert(
                R.id.btn_alert_price,
                R.id.radio_alert_down,
                "10000"
        );

        assertInvalidValue();
    }

    @Test
    public void validAlert_appearsInList() {

        submitAlert(
                R.id.btn_alert_percent,
                R.id.radio_alert_up,
                "5"
        );

        assertAlertCount(1);

        onView(withId(R.id.tv_alert_rule))
                .perform(scrollTo())
                .check(matches(isDisplayed()));

        scenario.onActivity(activity -> {

            TextInputLayout layout =
                    activity.findViewById(
                            R.id.alert_value_layout
                    );

            assertEquals(null, layout.getError());
        });
    }

    // Select alert type, direction and value, then save.
    private void submitAlert(
            int typeId,
            int directionId,
            String value) {

        onView(withId(typeId))
                .perform(scrollTo(), click());

        onView(withId(directionId))
                .perform(scrollTo(), click());

        onView(withId(R.id.alert_value_input))
                .perform(
                        scrollTo(),
                        replaceText(value),
                        closeSoftKeyboard()
                );

        onView(withId(R.id.btn_save_alert))
                .perform(scrollTo(), click());

        InstrumentationRegistry.getInstrumentation()
                .waitForIdleSync();
    }

    // Verify both the validation message and empty list.
    private void assertInvalidValue() {

        scenario.onActivity(activity -> {

            TextInputLayout layout =
                    activity.findViewById(
                            R.id.alert_value_layout
                    );

            assertNotNull(layout.getError());

            assertEquals(
                    activity.getString(
                            R.string.error_invalid_value
                    ),
                    layout.getError().toString()
            );
        });

        assertAlertCount(0);

        onView(withId(R.id.tv_alert_empty))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    // Verify the number of alerts in the RecyclerView.
    private void assertAlertCount(int expectedCount) {

        scenario.onActivity(activity -> {

            RecyclerView recyclerView =
                    activity.findViewById(
                            R.id.alerts_recycler_view
                    );

            assertNotNull(recyclerView.getAdapter());

            assertEquals(
                    expectedCount,
                    recyclerView.getAdapter().getItemCount()
            );
        });
    }
}