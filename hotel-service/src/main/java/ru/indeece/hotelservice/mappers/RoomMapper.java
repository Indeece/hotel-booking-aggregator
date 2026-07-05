package ru.indeece.hotelservice.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.indeece.hotelservice.dto.RoomCreateRequest;
import ru.indeece.hotelservice.dto.RoomDto;
import ru.indeece.hotelservice.entities.Room;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomMapper {
    RoomDto toDto(Room room);
    Room toEntity(RoomCreateRequest request);
}