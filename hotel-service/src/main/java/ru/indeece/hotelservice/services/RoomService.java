package ru.indeece.hotelservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.indeece.hotelservice.dto.RoomCreateRequest;
import ru.indeece.hotelservice.dto.RoomDto;
import ru.indeece.hotelservice.entities.Hotel;
import ru.indeece.hotelservice.entities.Room;
import ru.indeece.hotelservice.exceptions.ResourceNotFoundException;
import ru.indeece.hotelservice.mappers.RoomMapper;
import ru.indeece.hotelservice.repository.HotelRepository;
import ru.indeece.hotelservice.repository.RoomRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomMapper roomMapper;

    public RoomDto getRoomById(Long id) {
        return roomRepository.findById(id)
                .map(roomMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    public List<RoomDto> getRoomsByHotelId(Long hotelId) {
        return roomRepository.findByHotelId(hotelId).stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Transactional
    @CacheEvict(value = {"hotelsByCity", "hotelDetails"}, allEntries = true)
    public RoomDto createRoom(Long hotelId, RoomCreateRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));

        Room room = roomMapper.toEntity(request);
        room.setHotel(hotel);

        Room savedRoom = roomRepository.save(room);
        log.info("Created room {} for hotel {}", savedRoom.getRoomNumber(), hotelId);
        return roomMapper.toDto(savedRoom);
    }

    @Transactional
    @CacheEvict(value = {"hotelsByCity", "hotelDetails"}, allEntries = true)
    public RoomDto updateRoom(Long id, RoomCreateRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));

        room.setRoomNumber(request.roomNumber());
        room.setType(request.type());
        room.setPricePerNight(request.pricePerNight());
        room.setCapacity(request.capacity());

        log.info("Updated room with id {}", id);
        return roomMapper.toDto(roomRepository.save(room));
    }

    @Transactional
    @CacheEvict(value = {"hotelsByCity", "hotelDetails"}, allEntries = true)
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
        log.info("Deleted room with id {}", id);
    }
}