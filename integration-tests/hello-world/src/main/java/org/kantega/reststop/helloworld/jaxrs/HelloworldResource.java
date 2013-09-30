/*
 * Copyright 2013 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kantega.reststop.helloworld.jaxrs;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

/**
 *
 */
@Path("helloworld/{lang}")
public class HelloworldResource {

    @GET
    @Produces({"application/json", "application/xml", })
    @RolesAllowed("manager")
    public Hello hello(@PathParam(value = "lang") String lang) {


        String message = "Hello world";

        switch(lang) {
            case "no": message = "Hei verden";break;
            case "se": message = "Hej värden";break;
            case "fr": message = "Bonjour tout le monde";break;
        }


        return new Hello(message);
    }
}
