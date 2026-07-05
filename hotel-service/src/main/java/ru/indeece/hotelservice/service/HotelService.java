package ru.indeece.hotelservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.indeece.hotelservice.dto.HotelCreateRequest;
import ru.indeece.hotelservice.dto.HotelDto;
import ru.indeece.hotelservice.entities.Hotel;
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
                .orElseThrow(() -> new ResourceNotFoundException("Hotel wasn't found"));
    }

    @Transactional
    @CacheEvict(value = {"hotelsByCity", "hotelDetails"}, allEntries = true)
    public HotelDto createHotel(HotelCreateRequest request) {
        Hotel hotel = hotelMapper.toEntity(request);
        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Created hotel {}", savedHotel);
        return hotelMapper.toDto(savedHotel);
    }
}
