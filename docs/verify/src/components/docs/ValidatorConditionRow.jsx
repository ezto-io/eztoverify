import React, { useState } from 'react'

const ValidatorConditionRow = () => {
    const [type, setType] = useState("String");
    const options = ["String", "Number", "Date", "Boolean", "Array", "Object"];
    const style = {
        width: '11rem',
        padding: '0.3rem',
        borderRadius: '4px',
        marginTop: '6px',
        border: 'solid 1px #c9c9c9'
      };
  return (
    <tr>
        <td>
            Condition
            <div style={{display:'flex', alignItems: 'center', gap:"10px", marginTop:"1rem"}}>
                <label>Type</label>
                <select value={type} onChange={(event) => setType(event.target.value)} style={style} >
                    <option value="" disabled>Select type</option>
                    {options.map((option) => <option value={option}>{option}</option>)}
                </select>
            </div>
        </td>
        { type === "String" && <td>
            A <code>string</code> is a sequence of characters used to represent text. In various operations, you can compare, search, or validate these sequences based on specific criteria.
            <ul>
                <li><code>is equal to</code>: Compares the string exactly with another string. It returns true if both strings are identical in every character.</li>
                <li><code>isn't equal to</code>: Checks if the string is not identical to another string. It returns true if there are any differences between the two strings.</li>
                <li><code>contains</code>: Checks if the string includes a specific sequence of characters anywhere within it.</li>
                <li><code>doesn't contain</code>: Verifies that the string does not include a specific sequence of characters.</li>
                <li><code>starts with</code>: Determines if the string begins with a specific sequence of characters.</li>
                <li><code>doesn't start with</code>: Verifies that the string does not begin with a specific sequence of characters.</li>
                <li><code>ends with</code>: Checks if the string concludes with a specific sequence of characters.</li>
                <li><code>doesn't end with</code>: Verifies that the string does not conclude with a specific sequence of characters.</li>
                <li><code>matches regex</code>: Validates the string against a regular expression pattern. It returns true if the string conforms to the pattern.</li>
                <li><code>doesn't match regex</code>: Checks if the string does not conform to a given regular expression pattern.</li>
            </ul>
        </td> }
        { type === "Number" && <td>
            A <code>number</code> represents a numerical value that can be compared or evaluated based on mathematical conditions.
            <ul>
                <li><code>is equal to</code>: Compares the string exactly with another string. It returns true if both strings are identical in every character.</li>
                <li><code>isn't equal to</code>: Checks if the string is not identical to another string. It returns true if there are any differences between the two strings.</li>
                <li><code>is greater than</code>: Determines if the number is larger than another number.</li>
                <li><code>is less than</code>: Checks if the number is smaller than another number.</li>
                <li><code>is greater than or equal</code>: Evaluates if the number is larger than or the same as another number.</li>
                <li><code>is less than or equal</code>: Determines if the number is smaller than or the same as another number.</li>
            </ul>
        </td>}
        { type === "Date" && <td>
            A <code>date</code> represents a specific point in time and can be compared to other dates to determine chronological order.
            <ul>
                <li><code>is after</code>: Checks if the date occurs after another date.</li>
                <li><code>is before</code>: Verifies if the date occurs before another date.</li>
                <li><code>is after or equal</code>: Determines if the date is either after or exactly the same as another date.</li>
                <li><code>is before or equal</code>: Checks if the date is either before or exactly the same as another date.</li>
            </ul>
        </td>}
        { type === "Boolean" && <td>
            A <strong>boolean</strong> represents a true or false value, often used in conditional logic.
            <ul>
                <li><code>exists</code>: Checks if the boolean value is present (i.e., not null or undefined).</li>
                <li><code>doesn't exist</code>: Verifies that the boolean value is absent (i.e., null or undefined).</li>
                <li><code>is equal to</code>: Compares the boolean value to another boolean value.</li>
                <li><code>isn't equal to</code>: Checks if the boolean value is different from another boolean value.</li>
                <li><code>is true</code>: Determines if the boolean value is true.</li>
                <li><code>is false</code>: Verifies if the boolean value is false.</li>
            </ul>
        </td>}
        { type === "Array" &&  <td>
            An <strong>array</strong> is a collection of items that can be evaluated based on its contents or length.
            <ul>
                <li><code>contains</code>: Checks if the array includes a specific item.</li>
                <li><code>doesn't contain</code>: Verifies that a specific item is not included in the array.</li>
                <li><code>length equal to</code>: Compares the number of items in the array to a specified value.</li>
                <li><code>length not equal to</code>: Checks if the number of items in the array is different from a specified value.</li>
                <li><code>length greater than</code>: Determines if the number of items in the array is larger than a specified value.</li>
                <li><code>length greater than or equal</code>: Checks if the number of items in the array is larger than or the same as a specified value.</li>
                <li><code>length less than</code>: Verifies if the number of items in the array is smaller than a specified value.</li>
                <li><code>length less than or equal</code>: Determines if the number of items in the array is smaller than or the same as a specified value.</li>
            </ul>
        </td>}
        { type === "Object" && <td>
            An <strong>object</strong> is a complex data structure that contains key-value pairs. It can be evaluated based on its existence or content.
            <ul>
                <li><code>exists</code>: Checks if the object is present (i.e., not null or undefined).</li>
                <li><code>doesn't exist</code>: Verifies that the object is absent (i.e., null or undefined).</li>
                <li><code>is equal to</code>: Compares the object with another object to check if they are identical.</li>
                <li><code>isn't equal to</code>: Checks if the object is different from another object.</li>
            </ul>
        </td>}
    </tr>
  )
}

export default ValidatorConditionRow
