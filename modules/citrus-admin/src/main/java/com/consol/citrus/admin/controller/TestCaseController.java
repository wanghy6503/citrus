/*
 * Copyright 2006-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.admin.controller;

import com.consol.citrus.admin.launcher.ProcessMonitor;
import com.consol.citrus.admin.model.FileTreeModel;
import com.consol.citrus.admin.model.TestCaseDetail;
import com.consol.citrus.admin.model.TestCaseInfo;
import com.consol.citrus.admin.model.TestCaseType;
import com.consol.citrus.admin.service.ProjectService;
import com.consol.citrus.admin.service.TestCaseService;
import com.consol.citrus.admin.util.FileHelper;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.util.List;

/**
 * Controller manages test case related queries like get all tests
 * or getting a specific test case.
 * 
 * @author Christoph Deppisch
 */
@Controller
@RequestMapping("/testcase")
public class TestCaseController {

    @Autowired
    private ProcessMonitor processMonitor;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private FileHelper fileHelper;
    
    @RequestMapping(method = { RequestMethod.GET })
    @ResponseBody
    public List<TestCaseInfo> list() {
        return testCaseService.getTests(projectService.getActiveProject());
    }

    @RequestMapping(method = { RequestMethod.POST })
    @ResponseBody
    public ModelAndView list(@RequestParam("dir") String dir) {
        ModelAndView view = new ModelAndView("TestFileTree");

        FileTreeModel model = testCaseService.getTestFileTree(projectService.getActiveProject(), FilenameUtils.separatorsToSystem(fileHelper.decodeDirectoryUrl(dir, "")));

        if (StringUtils.hasText(model.getCompactFolder())) {
            view.addObject("compactFolder", FilenameUtils.separatorsToUnix(model.getCompactFolder()));
            view.addObject("baseDir", FilenameUtils.separatorsToUnix(fileHelper.decodeDirectoryUrl(dir, "")
                    + model.getCompactFolder() + File.separator));
        } else {
            view.addObject("baseDir", FilenameUtils.separatorsToUnix(fileHelper.decodeDirectoryUrl(dir, "")));
        }

        view.addObject("folders", model.getFolders());
        view.addObject("xmlFiles", model.getXmlFiles());
        view.addObject("javaFiles", model.getJavaFiles());

        return view;
    }

    @RequestMapping(value = "/count", method = { RequestMethod.POST })
    @ResponseBody
    public Long testCount() {
        return testCaseService.getTestCount(projectService.getActiveProject());
    }

    @RequestMapping(value="/details/{type}/{package}/{name}", method = { RequestMethod.GET })
    @ResponseBody
    public TestCaseDetail getTestDetail(@PathVariable("package") String testPackage, @PathVariable("name") String testName,
                                        @PathVariable("type") String type, @RequestParam(value = "method", required = false) String method) {
        if (StringUtils.hasText(method)) {
            return testCaseService.getTestDetail(projectService.getActiveProject(), testPackage, testName + "." + method, TestCaseType.valueOf(type.toUpperCase()));
        } else {
            return testCaseService.getTestDetail(projectService.getActiveProject(), testPackage, testName, TestCaseType.valueOf(type.toUpperCase()));
        }
    }
    
    @RequestMapping(value="/source/{type}/{package}/{name}", method = { RequestMethod.GET })
    @ResponseBody
    public String getSourceCode(@PathVariable("package") String testPackage, @PathVariable("name") String testName,
                                @PathVariable("type") String type) {
        return testCaseService.getSourceCode(projectService.getActiveProject(), testPackage, testName, TestCaseType.valueOf(type.toUpperCase()));
    }
    
    @RequestMapping(value="/execute/{package}/{name}", method = { RequestMethod.GET })
    @ResponseBody
    public ResponseEntity<String> executeTest(@PathVariable("package") String testPackage, @PathVariable("name") String testName,
                                              @RequestParam(value = "runConfiguration", required = true) String runConfigurationId, @RequestParam(value = "method", required = false) String method) {
        if (StringUtils.hasText(method)) {
            testCaseService.executeTest(projectService.getActiveProject(), testPackage, testName + "." + method, runConfigurationId);
        } else {
            testCaseService.executeTest(projectService.getActiveProject(), testPackage, testName, runConfigurationId);
        }

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @RequestMapping(value="/stop/{processId}", method = { RequestMethod.GET })
    @ResponseBody
    public ResponseEntity<String> stopTest(@PathVariable("processId") String processId) {
        processMonitor.stopProcess(processId);
        return new ResponseEntity<String>(HttpStatus.OK);
    }
}
