package me.alinizamani.byline.web.mapper;

import me.alinizamani.byline.domain.user.UserProfile;
import me.alinizamani.byline.web.dto.request.UpdateProfileRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfileFromRequest(UpdateProfileRequest request, @MappingTarget UserProfile profile);

}