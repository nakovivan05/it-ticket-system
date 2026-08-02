package com.ticketsystem.it_ticket_system.service;

import com.ticketsystem.it_ticket_system.dto.CategoryDTO;
import com.ticketsystem.it_ticket_system.dto.CreateCategoryDTO;
import com.ticketsystem.it_ticket_system.dto.UpdateCategoryDTO;
import com.ticketsystem.it_ticket_system.exception.CategoryNotFoundException;
import com.ticketsystem.it_ticket_system.exception.DuplicateCategoryNameException;
import com.ticketsystem.it_ticket_system.exception.ValidationException;
import com.ticketsystem.it_ticket_system.model.Category;
import com.ticketsystem.it_ticket_system.model.EntityType;
import com.ticketsystem.it_ticket_system.model.Operation;
import com.ticketsystem.it_ticket_system.repository.CategoryRepository;
import com.ticketsystem.it_ticket_system.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {
    @Mock
    CategoryRepository categoryRepository;

    @Mock
    TicketRepository ticketRepository;

    @Mock
    AuditLogService auditLogService;

    @InjectMocks
    CategoryService categoryService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCategoryById_WhenCategoryNotFound_ThrowsException()
    {
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(categoryId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCategoryById_WhenCategoryExists_ReturnsCategoryDTO()
    {
        Category category = new Category();
        Long categoryId = 1L;
        category.setId(categoryId);
        category.setName("Category 1");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        CategoryDTO categoryDTO = categoryService.getCategoryById(categoryId);

        assertEquals(categoryId, categoryDTO.getId());
        assertEquals("Category 1", categoryDTO.getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCategories_ReturnsList() {
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("Hardware");

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Software");

        when(categoryRepository.findAll()).thenReturn(List.of(category1, category2));

        List<CategoryDTO> result = categoryService.getAllCategories();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Hardware", result.get(0).getName());
        assertEquals(2L, result.get(1).getId());
        assertEquals("Software", result.get(1).getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCategoryByName_WhenCategoryExists_ReturnsCategoryDTO() {
        String categoryName = "Hardware";
        Category category = new Category();
        category.setId(1L);
        category.setName(categoryName);

        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.of(category));

        CategoryDTO result = categoryService.getCategoryByName(categoryName);

        assertEquals(categoryName, result.getName());
        assertEquals(1L, result.getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCategoryByName_WhenCategoryNotFound_ThrowsException() {
        String categoryName = "NonExistent";

        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryByName(categoryName));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategory_WhenValidData_ReturnsCategoryDTO() {
        CreateCategoryDTO dto = CreateCategoryDTO.builder()
                .name("Hardware")
                .description("Hardware issues")
                .build();

        when(categoryRepository.findByName("Hardware")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDTO result = categoryService.createCategory(dto);

        assertEquals("Hardware", result.getName());
        assertEquals("Hardware issues", result.getDescription());
        verify(auditLogService).auditLog(EntityType.CATEGORY, Operation.CREATE, "Category created: Hardware", any(), "admin");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCategory_WhenDuplicateName_ThrowsException() {
        CreateCategoryDTO dto = CreateCategoryDTO.builder()
                .name("Hardware")
                .description("Hardware issues")
                .build();

        Category existingCategory = new Category();
        existingCategory.setName("Hardware");

        when(categoryRepository.findByName("Hardware")).thenReturn(Optional.of(existingCategory));

        assertThrows(DuplicateCategoryNameException.class, () -> categoryService.createCategory(dto));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategory_WhenCategoryNotFound_ThrowsException() {
        Long categoryId = 999L;
        UpdateCategoryDTO dto = new UpdateCategoryDTO();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.updateCategory(categoryId, dto));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategory_WhenDuplicateName_ThrowsException() {
        Long categoryId = 1L;
        Category existingCategory = new Category();
        existingCategory.setId(categoryId);
        existingCategory.setName("OldName");

        UpdateCategoryDTO dto = UpdateCategoryDTO.builder()
                .name("NewName")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findByName("NewName")).thenReturn(Optional.of(new Category()));

        assertThrows(DuplicateCategoryNameException.class, () -> categoryService.updateCategory(categoryId, dto));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCategory_WhenValidData_ReturnsUpdatedCategoryDTO() {
        Long categoryId = 1L;
        Category existingCategory = new Category();
        existingCategory.setId(categoryId);
        existingCategory.setName("OldName");
        existingCategory.setDescription("Old description");

        UpdateCategoryDTO dto = UpdateCategoryDTO.builder()
                .name("NewName")
                .description("New description")
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.findByName("NewName")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryDTO result = categoryService.updateCategory(categoryId, dto);

        assertEquals("NewName", result.getName());
        assertEquals("New description", result.getDescription());
        verify(auditLogService).auditLog(EntityType.CATEGORY, Operation.UPDATE, "Category updated: NewName", categoryId, "admin");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategory_WhenCategoryNotFound_ThrowsException() {
        Long categoryId = 999L;

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(categoryId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategory_WhenReferencedByTickets_ThrowsException() {
        Long categoryId = 1L;
        Category existingCategory = new Category();
        existingCategory.setId(categoryId);
        existingCategory.setName("Hardware");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(ticketRepository.existsByCategoryId(categoryId)).thenReturn(true);

        assertThrows(ValidationException.class, () -> categoryService.deleteCategory(categoryId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCategory_WhenValidCategory_DeletesCategory() {
        Long categoryId = 1L;
        Category existingCategory = new Category();
        existingCategory.setId(categoryId);
        existingCategory.setName("Hardware");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(existingCategory));
        when(ticketRepository.existsByCategoryId(categoryId)).thenReturn(false);

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository).delete(existingCategory);
        verify(auditLogService).auditLog(EntityType.CATEGORY, Operation.DELETE, "Category deleted: Hardware", categoryId, "admin");
    }
}
