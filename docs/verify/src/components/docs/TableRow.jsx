import React, {useState, useEffect} from 'react'

function TableRow({title, defaultSelection, jsonData, defaultDesc}) {
    const [selectedData, setSelectedData] = useState(defaultSelection);
    const [selectedChild, setSelectedChild] = useState("");
    const [description, setDescription] = useState("");

    const processText = (text) => {
        return text.replaceAll("**", " ");
    };
      
    const splitText = (text) => {
        return text.split(" ");
    };
    
    const isBold = (text) => {
        return text.startsWith("**");
    };
    
    const style = {
        width: '11rem',
        padding: '0.3rem',
        borderRadius: '4px',
        marginTop: '6px',
        border: 'solid 1px #c9c9c9'
      };

    useEffect(() => {
        setDescription(jsonData[selectedData].desc);
    }, [selectedData]);

    const handleChange = (value) => {
        setSelectedData(value)
        setSelectedChild(Object.keys(jsonData[value].child)[0])
    }

    return (
        <>
            <tr>
                <td>
                    <p>{title}</p>
                    <select value={selectedData} onChange={(event) => handleChange(event.target.value)} style={style} >
                        <option value="" disabled>Select {title}</option>
                        {Object.keys(jsonData).map((option) => <option value={option}>{option}</option>)}
                    </select>
                </td>
                <td>
                    <p>
                        {
                            splitText(jsonData[selectedData].desc).map((text) => {
                                return isBold(text) ? 
                                <span className="prop-name-bold">{processText(text)}</span> : 
                                <span>{" "}{processText(text)}</span>
                            })
                        }
                    </p>
                </td>
            </tr>
            {Object.keys(jsonData[selectedData].child).length < 2 
            ? (
                <tr>
                    <td>
                        <p>{Object.keys(jsonData[selectedData].child)[0]}</p>
                    </td>
                    <td>
                        <p>
                            {
                                splitText(jsonData[selectedData].child[Object.keys(jsonData[selectedData].child)[0]].desc).map((text) => {
                                    return isBold(text) ? 
                                    <span className="prop-name-bold">{processText(text)}</span> : 
                                    <span>{" "}{processText(text)}</span>
                                })
                            }
                        </p>
                    </td>
                </tr>
            ) :
            (<tr>
                <td>
                <select value={selectedChild} onChange={(event) => setSelectedChild(event.target.value)} style={style} >
                    <option value="" disabled>Select {title}</option>
                    {Object.keys(jsonData[selectedData].child).map((option) => <option value={option}>{option}</option>)}
                </select>
                </td>
                <td>
                    {
                        splitText(jsonData[selectedData].child[selectedChild].desc).map((text) => {
                            return isBold(text) ? 
                            <span className="prop-name-bold">{processText(text)}</span> : 
                            text.startsWith("https") ? <code>{processText(text)}</code> :
                            <span>{" "}{processText(text)}</span>
                        })
                    }
                </td>
            </tr>) 
           }
        </>
    )
}

export default TableRow