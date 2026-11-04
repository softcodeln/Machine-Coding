package com.lucky.parkinglot.payment;

import com.lucky.parkinglot.gateway.RazorpayGatewayAdapter;
import com.lucky.parkinglot.gateway.RazorpayPaymentGateway;
import com.lucky.parkinglot.gateway.StripeGatewayAdapter;
import com.lucky.parkinglot.gateway.StripePaymentGateway;
import com.lucky.parkinglot.model.Customer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    private final Customer customer = new Customer("Ravi");

    @Test
    void stripeAdapterReordersArgumentsForStripe() {
        RecordingStripe stripe = new RecordingStripe();

        assertThat(new CardPayment(new StripeGatewayAdapter(stripe), "US").pay(customer, 100)).isTrue();

        assertThat(stripe.amount).isEqualTo(100);
        assertThat(stripe.country).isEqualTo("US");
    }

    @Test
    void razorpayAdapterCallsPayingAndPropagatesFailure() {
        RecordingRazorpay razorpay = new RecordingRazorpay();

        assertThat(new UpiPayment(new RazorpayGatewayAdapter(razorpay), "IN").pay(customer, 250)).isFalse();

        assertThat(razorpay.amount).isEqualTo(250);
        assertThat(razorpay.countryCode).isEqualTo("IN");
    }

    @Test
    void cashPaymentAlwaysSucceeds() {
        assertThat(new CashPayment().pay(customer, 50)).isTrue();
    }

    private static class RecordingStripe extends StripePaymentGateway {

        private Integer amount;
        private String country;

        @Override
        public boolean pay(Integer amount, String country) {
            this.amount = amount;
            this.country = country;
            return true;
        }
    }

    private static class RecordingRazorpay extends RazorpayPaymentGateway {

        private String countryCode;
        private Integer amount;

        @Override
        public boolean paying(String countryCode, Integer amount) {
            this.countryCode = countryCode;
            this.amount = amount;
            return false;
        }
    }
}
