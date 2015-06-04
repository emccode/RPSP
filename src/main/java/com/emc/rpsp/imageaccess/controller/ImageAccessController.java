package com.emc.rpsp.imageaccess.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.emc.rpsp.imageaccess.service.ImageAccessService;

@Controller
public class ImageAccessController {

	@Autowired
	private ImageAccessService imageAccessService;

	@RequestMapping(value = "/image-access/enable", params = {"clusterId", "groupId", "copyId"}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<HttpStatus> enableImageAccess(@RequestParam("clusterId") Long clusterId, @RequestParam("groupId") Long groupId, @RequestParam("copyId") Integer copyId) {
		imageAccessService.enableImageAccess(clusterId, groupId, copyId);
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	
	@RequestMapping(value = "/image-access/disable", params = {"clusterId", "groupId", "copyId"}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<HttpStatus> disableImageAccess(@RequestParam("clusterId") Long clusterId, @RequestParam("groupId") Long groupId, @RequestParam("copyId") Integer copyId) {
		imageAccessService.disableImageAccess(clusterId, groupId, copyId);
		return new ResponseEntity<>(HttpStatus.OK);
	}

}