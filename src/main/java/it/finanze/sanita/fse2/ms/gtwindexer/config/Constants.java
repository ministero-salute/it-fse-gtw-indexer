/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.config;

/**
 * 
 *
 * Constants application.
 */
public final class Constants {
 
	
	public static final class Profile {
		
		/**
		 * Test profile.
		 */
		public static final String TEST = "test";
		
		public static final String TEST_PREFIX = "test_";

		/**
		 * Dev profile.
		 */
		public static final String DEV = "dev";

		/**
		 * Docker profile.
		 */
		public static final String DOCKER = "docker";

		/** 
		 * Constructor.
		 */
		private Profile() {
			//This method is intentionally left blank.
		}

	}
 
	/**
	 *	Constants.
	 */
	private Constants() {

	}

}
