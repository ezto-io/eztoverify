---
id: HelperFunctions
title: Helper Functions
sidebar_label: Helper Functions
--- 

Helper functions enhance the customization of workflow modules such as [API Request](/docs/EV/WorkflowModules/APIRequest), [Dynamic Form](/docs/EV/WorkflowModules/DynamicForm), [Validator](/docs/EV/WorkflowModules/Validator), and [Custom Response](/docs/EV/WorkflowModules/CustomResponse). These functions enable the pre-filling of values, increasing the flexibility and efficiency of workflows.  

By utilizing these helper functions, workflows can be easily customized to meet specific requirements, minimizing manual effort and simplifying configuration. They are particularly useful for managing custom claims, transforming data, and preparing requests in a seamless and automated manner.  

With helper functions, workflows become highly dynamic and adaptable, allowing for smooth data transformation and effortless customization to accommodate a wide range of use cases. 

## Available Functions 

### *1* GenerateUUID

The function (`${GenerateUUID}`) generates a **universally unique identifier (UUID).**   
   - Example Usage:  
     ```json  
     {  
         "id": "${GenerateUUID}"  
     }  
     ```  
   - Expected Output:  
     ```json  
     {  
         "id": "550e8400-e29b-41d4-a716-446655440000"  
     }  
     ```  
     
<br/>

### *2* Replace 

The function `${Replace(${value}, {value1: newvalue1, value2: newvalue2})}` maps specific input values to corresponding replacements.  
   - Example Usage:  
     ```json  
     {  
         "gender_value": "${Replace(${env.gender}, {M: 1, F: 2})}"  
     }  
     ```  
   - Expected Output:  
     ```json  
     {  
         "gender_value": "2"  // If the gender value is 'F', it will be replaced with '2'.  
     }  
     ```  

<br/>

### *3* Date 

The function `${Date(${env.dob}, dd-MM-yyyy)}` formats a given date into a specific pattern.  
   - Example Usage:  
     ```json  
     {  
         "dob_formatted": "${Date(${env.dob}, dd-MM-yyyy)}"  
     }  
     ```  
   - Expected Output:  
     ```json  
     {  
         "dob_formatted": "29-01-1999"  
     }  
     ```  

<br/>

### *4* ToLowerCase

The function `${ToLowerCase(${env.last_name})}` converts a string to lowercase.  
   - Example Usage:  
     ```json  
     {  
         "last_name_lowercase": "${ToLowerCase(${env.last_name})}"  
     }  
     ```  
   - Expected Output:  
     ```json  
     {  
         "last_name_lowercase": "doe"  
     }  
     ```  

<br/>   

### *5* ToUpperCase

The function `${ToUpperCase(${env.first_name})}` converts a string to uppercase.  
   - Example Usage:  
     ```json  
     {  
         "first_name_uppercase": "${ToUpperCase(${env.first_name})}"  
     }  
     ```  
   - Expected Output:  
     ```json  
     {  
         "first_name_uppercase": "JANE"  
     }  
     ```  

<br/> 

<br/>

### *6* Split  

The function `${Split(${value}, start_index, end_index)}` splits a string from the given value based on the specified start and end index.

   - Example Usage:  
     ```json  
     {  
    "full_name": "${Split(${env.full_name}, 3, 9)}"  
     }    
     ```  

  > Assumed input, `env.full_name = "Alexandra"`

   - Expected Output:  
     ```json  
     {  
    "full_name": "exandr"  
     }  

     ```  
  > The split starts from index 3 (zero-based) and extracts characters up to index 9, resulting in "exandr".  

<br/>  

## Practical Usage 

Assume the following **environment variables** are set within a workflow:

```json  
{  
    "full_name": "Alexandra Smith",  
    "first_name": "Alexandra",  
    "last_name": "Smith",  
    "dob": "1985-06-15",  
    "gender": "F"  
}  
```  
<br/>

##### Here’s an example of using the helper functions within the workflow:
 
Example Usage:  
```json  
{  
    "first_name_uppercase": "${ToUpperCase(${env.first_name})}",  
    "last_name_lowercase": "${ToLowerCase(${env.last_name})}",  
    "dob_formatted": "${Date(${env.dob}, dd-MM-yyyy)}",  
    "gender_value": "${Replace(${env.gender}, {M: 1, F: 2})}",  
    "unique_id": "${GenerateUUID}",  
    "partial_name": "${Split(${env.full_name}, 2, 6)}"  
}   
```  

Expected Output:  
```json  
{  
    "first_name_uppercase": "ALEXANDRA",  
    "last_name_lowercase": "smith",  
    "dob_formatted": "15-06-1985",  
    "gender_value": "2",  
    "unique_id": "6f9a3b33-54d5-4e0e-bc82-47a0c4fbc897",  
    "partial_name": "exan"  
}  
  
```  