package com.mapper;

import com.entity.Project;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ProjectMapper {
    List<Project> selectAll();
    Project selectByIdWithEmps(@Param("projectId") Integer projectId);
    int insert(Project project);
}
