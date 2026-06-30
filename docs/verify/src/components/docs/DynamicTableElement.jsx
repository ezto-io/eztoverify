import React, {useState, useEffect} from 'react'

function DynamicTableElement({title, defaultSelection, defaultDescription, jsonData}) {
    const [selectedData, setSelectedData] = useState(defaultSelection);
    const [description, setDescription] = useState(defaultSelection);

    const style = {
        width: '11rem',
        padding: '0.3rem',
        borderRadius: '4px',
        marginTop: '6px',
        border: 'solid 1px #c9c9c9'
      };

    useEffect(() => {
        if(selectedData.trim().length == 0){
            setDescription(defaultDescription);
        }else{
            setDescription(jsonData[selectedData]);
        }
    }, [selectedData]);

    return (
        <tr>
            <td>
                <strong>{title}</strong>
                <select value={selectedData} onChange={(event) => setSelectedData(event.target.value)} style={style} >
                    <option value="" disabled>Select {title}</option>
                    {Object.keys(jsonData).map((option) => <option value={option}>{option}</option>)}
                </select>
            </td>
            <td>{description}</td>
        </tr>
    )
}

export default DynamicTableElement