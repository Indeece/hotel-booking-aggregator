package ru.indeece.hotelservice.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.indeece.hotelservice.dto.HotelCreateRequest;
import ru.indeece.hotelservice.dto.HotelDto;
import ru.indeece.hotelservice.dto.RoomDto;
import ru.indeece.hotelservice.entities.Hotel;
import ru.indeece.hotelservice.entities.Room;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HotelMapper {
    HotelDto toDto(Hotel hotel);
    RoomDto toDto(Room room);
    Hotel toEntity(HotelCreateRequest request);
}