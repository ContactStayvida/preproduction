package com.stayvida.backend.service;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stayvida.backend.repository.ledgerRepository;

@Service
public class ledgerService {
    @Autowired
    private ledgerRepository ledgerRepository;

    public List<Map<String, Object>> getLedgerByHotel(String hotelId, String type) {
        return ledgerRepository.fetchLedger(hotelId, type);
    }

    public Map<String, Object> getFinancialSummary(String hotelId, Integer month, Integer year) {

        // Default month/year to current if not provided
        LocalDate now = LocalDate.now();
        int m = (month != null) ? month : now.getMonthValue();
        int y = (year != null) ? year : now.getYear();

        // Convert month number to month name
        String monthName = Month.of(m).name(); // Returns "FEBRUARY"
        monthName = monthName.charAt(0) + monthName.substring(1).toLowerCase(); // "February"

        // Fetch data from repository
        Map<String, Object> summary = ledgerRepository.fetchFinancialSummary(hotelId, m, y);

        // Replace numeric month with name
        summary.put("month", monthName);

        return summary;
    }
}
