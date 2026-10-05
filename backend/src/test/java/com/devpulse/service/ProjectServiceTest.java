package com.devpulse.service;

import com.devpulse.dto.ProjectRequest;
import com.devpulse.entity.Project;
import com.devpulse.entity.User;
import com.devpulse.exception.ApiException;
import com.devpulse.repository.ProjectRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.devpulse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldCreateProjectForUser() {
        User user = new User();
        user.setId(10L);
        user.setEmail("owner@example.com");

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectRequest request = new ProjectRequest();
        request.setName("billing-service");
        request.setDescription("Payments service");
        request.setRepository("https://github.com/demo/billing-service");
        request.setEnvironment("development");

        Project project = projectService.createProject(10L, request);

        assertThat(project.getName()).isEqualTo("billing-service");
        assertThat(project.getOwner().getId()).isEqualTo(10L);
        assertThat(project.getEnvironment()).isEqualTo("development");
    }

    @Test
    void shouldRejectProjectCreationWhenOwnerDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ProjectRequest request = new ProjectRequest();
        request.setName("orphan-service");
        request.setEnvironment("development");

        assertThatThrownBy(() -> projectService.createProject(99L, request))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void shouldListProjectsAccessibleToUser() {
        Project project = new Project();
        project.setName("billing-service");

        when(projectRepository.findAccessibleByUserId(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(project)));

        var projects = projectService.listProjectsAccessibleToUser(10L, 0, 20);

        assertThat(projects.getContent()).hasSize(1);
        assertThat(projects.getContent().get(0).getName()).isEqualTo("billing-service");
    }
}
