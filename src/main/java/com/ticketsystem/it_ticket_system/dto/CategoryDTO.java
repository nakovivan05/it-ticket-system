package com.ticketsystem.it_ticket_system.dto;

import com.ticketsystem.it_ticket_system.model.Category;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryDTO {
    private Long id;
    private String name;
    private String description;

    public static CategoryDTO fromEntity(Category category){
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

}
