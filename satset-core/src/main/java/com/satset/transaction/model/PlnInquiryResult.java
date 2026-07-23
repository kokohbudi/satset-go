package com.satset.transaction.model;

/** Read model for prepaid PLN customer-name inquiry — mirrors Digiflazz's inquiry-pln response, provider-agnostic. */
public record PlnInquiryResult(String customerName, String meterNo, String segmentPower,
        String rc, String message) {

    public boolean ok() {
        return "00".equals(rc);
    }
}
