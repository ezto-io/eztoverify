
package org.keycloak.common.models;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class IPResponse {

    private Integer status;
    private Body body;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public String getLocation(){
        if(this.status != 200)
        {
            return "Unknown Location";
        }
        else {
            String ipLocation = this.body.getCity()+", "+this.body.getCountry()+", "+this.body.getContinent();
            ArrayList<String> locationDetails = new ArrayList<>(Arrays.asList(ipLocation.split(",")));
            locationDetails.removeAll(Collections.singleton(null));
            locationDetails.removeAll(Collections.singleton("null"));
            locationDetails.removeAll(Collections.singleton(""));

            return String.join(",", locationDetails);
        }
    }
}
