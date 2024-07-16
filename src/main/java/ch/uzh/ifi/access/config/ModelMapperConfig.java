package ch.uzh.ifi.access.config;

import ch.uzh.ifi.access.model.Assignment;
import ch.uzh.ifi.access.model.Course;
import ch.uzh.ifi.access.model.Submission;
import ch.uzh.ifi.access.model.Task;
import ch.uzh.ifi.access.model.constants.Visibility;
import ch.uzh.ifi.access.model.dto.AssignmentDTO;
import ch.uzh.ifi.access.model.dto.CourseDTO;
import ch.uzh.ifi.access.model.dto.SubmissionDTO;
import ch.uzh.ifi.access.model.dto.TaskDTO;
import org.apache.commons.lang3.ObjectUtils;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {
    // TODO: convert to Kotlin. This is very tricky, because ModelMapper is very Java-specific. Maybe replace it.
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setPropertyCondition(context -> ObjectUtils.isNotEmpty(context.getSource()))
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setSkipNullEnabled(true);
        modelMapper.typeMap(CourseDTO.class, Course.class)
                .addMappings(mapping -> mapping.skip(CourseDTO::getAssignments, Course::setAssignments));
        modelMapper.typeMap(AssignmentDTO.class, Assignment.class)
                .addMappings(mapping -> mapping.skip(AssignmentDTO::getTasks, Assignment::setTasks));
        modelMapper.typeMap(TaskDTO.class, Task.class)
                .addMappings(mapping -> mapping.skip(TaskDTO::getFiles, Task::setFiles));
        modelMapper.typeMap(SubmissionDTO.class, Submission.class)
                .addMappings(mapping -> mapping.skip(Submission::setFiles));
        modelMapper.createTypeMap(String.class, Visibility.class)
                .setConverter(context -> Visibility.valueOf(context.getSource().toUpperCase()));
        return modelMapper;
    }
}
