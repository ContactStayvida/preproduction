package com.stayvida.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.stayvida.backend.dto.GroupedRoomDTO;
import com.stayvida.backend.dto.HotelDTO;
import com.stayvida.backend.dto.RoomBasicDTO;
import com.stayvida.backend.dto.RoomDTO;

@Service
public class RoomService {

    public void groupRoomsByType(HotelDTO hotel) {

        List<RoomDTO> rawRooms = hotel.getRawRooms();

        if (rawRooms == null || rawRooms.isEmpty()) {
            hotel.setRooms(List.of());
            return;
        }

        Map<String, List<RoomDTO>> grouped = rawRooms.stream()
                .collect(Collectors.groupingBy(RoomDTO::getType));

        List<GroupedRoomDTO> groupedRooms = new ArrayList<>();

        for (Map.Entry<String, List<RoomDTO>> entry : grouped.entrySet()) {

            List<RoomDTO> roomList = entry.getValue();
            RoomDTO sample = roomList.get(0);

            GroupedRoomDTO dto = new GroupedRoomDTO();
            dto.setType(entry.getKey());

            dto.setRooms(
                    roomList.stream()
                            .map(r -> new RoomBasicDTO(r.getRoomId(), r.getRoom_NO()))
                            .toList());

            dto.setPrice(sample.getPrice());
            dto.setPlatformCharges(sample.getPlatformCharges());
            dto.setTaxRate(sample.getTaxRate());
            dto.setAdvanceRate(sample.getAdvanceRate());
            dto.setTotalAmount(sample.getTotalAmount());
            dto.setAdvanceAmount(sample.getAdvanceAmount());

            dto.setAdultsMax(sample.getAdultsMax());
            dto.setChildrenMax(sample.getChildrenMax());
            dto.setBedCount(sample.getBedCount());

            dto.setFeatures(sample.getFeatures());
            dto.setRoomImages(sample.getRoomImages());

            groupedRooms.add(dto);
        }

        hotel.setRooms(groupedRooms); // ✅ correct type
    }
}
