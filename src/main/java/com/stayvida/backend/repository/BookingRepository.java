package com.stayvida.backend.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.stayvida.backend.dto.BookingEmailDTO;

@Repository
public class BookingRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public BookingEmailDTO getBookingForEmail(String bookingId) {

        String sql = """
                SELECT
                b.booking_ID,
                b.checkIn,
                b.checkOut,
                b.room_NO,
                b.totalAmount,
                b.platformFee,
                b.tax_amount,
                b.payment_amount,
                b.payment_Status,
                b.payment_type,
                b.name,
                b.phone_no,
                h.name AS hotel_name

                FROM bookings b
                JOIN hotels h ON b.hotel_ID = h.hotel_ID
                WHERE b.booking_ID = ?
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {

            BookingEmailDTO dto = new BookingEmailDTO();

            dto.setBookingId(rs.getString("booking_ID"));
            dto.setHotelName(rs.getString("hotel_name"));

            dto.setCheckIn(rs.getDate("checkIn").toLocalDate());
            dto.setCheckOut(rs.getDate("checkOut").toLocalDate());

            dto.setRoomNo(rs.getInt("room_NO"));

            dto.setTotalAmount(rs.getBigDecimal("totalAmount"));
            dto.setPlatformFee(rs.getBigDecimal("platformFee"));
            dto.setTaxAmount(rs.getBigDecimal("tax_amount"));

            dto.setPaidAmount(rs.getBigDecimal("payment_amount"));

            dto.setPendingAmount(
                    rs.getBigDecimal("totalAmount")
                            .subtract(rs.getBigDecimal("payment_amount")));

            dto.setPaymentStatus(rs.getString("payment_Status"));
            dto.setPaymentType(rs.getString("payment_type"));

            dto.setGuestName(rs.getString("name"));
            dto.setPhone(rs.getString("phone_no"));

            return dto;

        }, bookingId);
    }
}