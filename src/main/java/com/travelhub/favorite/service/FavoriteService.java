package com.travelhub.favorite.service;

import com.travelhub.common.exception.DuplicateResourceException;
import com.travelhub.common.response.PageResponse;
import com.travelhub.common.util.Pages;
import com.travelhub.favorite.dto.FavoriteResponse;
import com.travelhub.favorite.entity.Favorite;
import com.travelhub.favorite.entity.FavoriteId;
import com.travelhub.favorite.repository.FavoriteRepository;
import com.travelhub.place.dto.PlaceSummaryResponse;
import com.travelhub.place.mapper.PlaceMapper;
import com.travelhub.place.service.PlaceService;
import com.travelhub.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FavoriteService {
    private final FavoriteRepository repository;
    private final UserService users;
    private final PlaceService places;
    private final PlaceMapper placeMapper;

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("@access.owns(#userId)")
    public FavoriteResponse add(Long userId, Long placeId) {
        var user = users.lock(userId);
        var place = places.require(placeId);
        var id = new FavoriteId(userId, placeId);
        if (repository.existsById(id)) throw new DuplicateResourceException("Place is already in favorites");
        Favorite favorite = new Favorite();
        favorite.setId(id);
        favorite.setUser(user);
        favorite.setPlace(place);
        Favorite saved = repository.saveAndFlush(favorite);
        log.info("Added favorite for user {} and place {}", userId, placeId);
        return new FavoriteResponse(userId, placeId, saved.getCreatedAt());
    }

    @Transactional
    @org.springframework.security.access.prepost.PreAuthorize("@access.owns(#userId)")
    public void remove(Long userId, Long placeId) {
        users.lock(userId);
        places.require(placeId);
        repository.findById(new FavoriteId(userId, placeId)).ifPresent(repository::delete);
        repository.flush();
        log.info("Removed favorite for user {} and place {}", userId, placeId);
    }

    @org.springframework.security.access.prepost.PreAuthorize("@access.owns(#userId)")
    public PageResponse<PlaceSummaryResponse> list(Long userId, Pageable pageable) {
        users.require(userId);
        Pageable checked = Pages.checked(pageable, "createdAt", "place.name", "place.averageRating", "id.placeId");
        Sort sort = checked.getSort();
        if (sort.getOrderFor("id.placeId") == null) sort = sort.and(Sort.by("id.placeId"));
        return PageResponse.from(repository.findByUserId(userId,
                PageRequest.of(checked.getPageNumber(), checked.getPageSize(), sort))
                .map(favorite -> placeMapper.toSummary(favorite.getPlace())));
    }
}
