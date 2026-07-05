package ru.indeece.hotelservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.indeece.hotelservice.dto.HotelCreateRequest;
import ru.indeece.hotelservice.dto.HotelDto;
import ru.indeece.hotelservice.entities.Hotel;
import ru.indeece.hotelservice.exceptions.ResourceNotFoundException;
import ru.indeece.hotelservice.mappers.HotelMapper;
import ru.indeece.hotelservice.repository.HotelRepository;

import java.util.List;

@Service
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    public HotelService(HotelRepository hotelRepository, HotelMapper hotelMapper) {
        this.hotelRepository = hotelRepository;
        this.hotelMapper = hotelMapper;
    }

    @Cacheable(value = "hotelsByCity", key = "#city")
    public List<HotelDto> findHotelsByCity(String city) {
        log.info("Finding hotels by city {}", city);
        return hotelRepository.findByCity(city).stream()
                .map(hotelMapper::toDto)
                .toList();
    }

    @Cacheable(value = "hotelDetails", key = "#id")
    public HotelDto findHotelById(Long id) {
        log.info("Finding hotel by id {}", id);
        return hotelRepository.findById(id)
                .map(hotelMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));
    }

    @Transactional
    @CacheEvict(value = {"hotelsByCity", "hotelDetails"}, allEntries = true)
    public HotelDto createHotel(HotelCreateRequest request) {
        Hotel hotel = hotelMapper.toEntity(request);
        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Created hotel with id {}", savedHotel.getId());
        return hotelMapper.toDto(savedHotel);
    }

    @Transactional
    @CacheEvict(value = {"hotelsByCity", "hotelDetails"}, allEntries = true)
    public HotelDto updateHotel(Long id, HotelCreateRequest request) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + id));

        hotel.setName(request.name());
        hotel.setDescription(request.description());
        hotel.setCity(request.city());
        hotel.setAddress(request.address());
        hotel.setStars(request.stars());

        log.info("Updated hotel with id {}", id);
        return hotelMapper.toDto(hotelRepository.save(hotel));
    }

    @Transactional
    @CacheEvict(value = {"hotelsByCity", "hotelDetails"}, allEntries = true)
    public void deleteHotel(Long id) {
        if (!hotelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hotel not found with id: " + id);
        }
        hotelRepository.deleteById(id);
        log.info("Deleted hotel with id {}", id);
    }
}