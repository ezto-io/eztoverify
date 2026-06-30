import React, {useState, useEffect} from "react";
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import { useSpeechSynthesis } from 'react-speech-hooks';
import Fuse from 'fuse.js'
import data from  './glossary.json';
import './glossary.css';

export default function Glossary() {
    
    const {siteConfig} = useDocusaurusContext();
    const { speak, cancel, speaking, supported, voices } = useSpeechSynthesis();
    const [speakingTitle, setSpeakingTitle] =  useState('');
    const [resultTitle, setResultTitle] = useState(['Auth', 'App', 'Workspace', 'Provider']);
    const [resultDesc, setResultDesc] = useState([]);
    const [initialDec, setInitialDesc] = useState(data);
    const [initialDecCopy, setInitialDescCopy] = useState(data);
    const [searchInput, setSearchInput] = useState('');
    const [isAlphabetFilter, setIsAlphabetFilter] = useState(false);
    const alphabets = ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'];

    // text processing
    const processText = (text) => {
        return text.replaceAll("**", " ");
    };
      
    const splitText = (text) => {
        return text.split(" ");
    };
      
    const isBold = (text) => {
        return text.startsWith("**");
    };
      
    const isLink = (text) => {
        return text.startsWith("[");
    };

    const textProcessing = (desc) => {
        return splitText(desc).map((text) => {
            return isBold(text) ? (
              <span className="prop-name-bold">{processText(text)}</span>
            ) : isLink(text) ? (
              <Link to={text.split("(")[1].replace(")", "")}>
                {text.split("(")[0].replace("[", "").replace("]", " ")}
              </Link>
            ) : (
              `${text} `
            );
        })
    } 

    const setSearchInputValue = (value) => {
        setIsAlphabetFilter(false);
        setSearchInput(value)
    }

    const alphabetFilter = (alphabet) => {
        setIsAlphabetFilter(true);
        setSearchInput(alphabet);
        setResultDesc([]);
        setInitialDesc(initialDecCopy.filter(obj => obj.title.startsWith(alphabet)))
        alphabets.map((result) => {
            var individualLetter = document.getElementById(`alphabet-${result}`)
            individualLetter.classList.remove('letter-active')
        }) 
        var letter = document.getElementById(`alphabet-${alphabet}`)
        if(!letter.classList.contains('letter-active')){
            letter.classList.add('letter-active')
        }
    }

    const fuseOptions = {
        includeScore: true,
        findAllMatches: false,
        threshold: 0.2,
        keys: [
          "title"
        ]
    };

    const fuse = new Fuse(data, fuseOptions);

    // Description card filter
    useEffect(()=>{
        if(!isAlphabetFilter){
            setInitialDesc(initialDecCopy);
            alphabets.map((result) => {
                var letter = document.getElementById(`alphabet-${result}`)
                letter.classList.remove('letter-active')
            })   
            setResultDesc(fuse.search(searchInput));  
            setIsAlphabetFilter(false);
        }
    }, [searchInput])

    // Alphabet filter
    useEffect(() => {
        if(resultDesc.length && searchInput.length){
            resultDesc.map((result) => {
                var letter = document.getElementById(`alphabet-${result.item.title[0]}`)
                if(!letter.classList.contains('letter-active')){
                    letter.classList.add('letter-active')
                }
            })
        }
    }, [resultDesc])

    // audio for description
    const triggerSpeak = (title, speakText) => {
        var voice ={
            text: speakText.replace(/[*\[\]]|\([^)]*\)/g, ''),
            rate: 0.7,
            pitch: 1,
            voice: voices[0]
        }
        if(!speaking && speakingTitle !== title){
            setSpeakingTitle(title);
            speak(voice)
        }else if(speaking && speakingTitle !== title){
            setSpeakingTitle(title);
            cancel();
            setTimeout(() =>{
                speak(voice)
            }, 1000)
        }else{
            setSpeakingTitle('');
            cancel();
        }
    }

  return (
    <Layout title={`Hello from ${siteConfig.title}`} description="Description will go into a meta tag in <head />">
    <div className="glossary-layout">
        {/* Banner */}
        <div className="glossary-banner">
            <img src="/verify/img/glossary-icons/banner-ring-left.svg" className="glossary-banner-left-ring-icon" alt="" />
            <img src="/verify/img/glossary-icons/banner-ring-right.svg" className="glossary-banner-right-ring-icon" alt="" />
            <div className="glossary-banner-title">Identity Gl
            <img src="/verify/img/glossary-icons/banner-search.svg" className="glossary-banner-search-icon" alt="" />
            ssary</div>
            <div className="glossary-banner-desc">We've put together a glossary of identity terms for newcomers and seasoned developers, alike. Hopefully this helps put any identity terminology confusion to rest.</div>
        </div>

        {/* Search */}
        <div className="glossary-search">
            <img src="/verify/img/glossary-icons/search-icon.svg" className="glossary-search-icon" alt="" />
            <input type="text" className="glossary-search-input" placeholder="search..." value={searchInput} onChange={(e) => setSearchInputValue(e.target.value)} />
        </div>

        {/* search results */}
        <div className="glossary-search-result">
            {/* title result */}
            <div className="title-result">
                {resultTitle.length !== 0 && resultTitle.map((title,index)=>
                    <div key={index} className="title-result-card" onClick={() => setSearchInputValue(resultTitle[index])}>{title}</div>
                )}
            </div>
            {/* alphabet result */}
            <div className="alphabet-result">
                {alphabets.map((letter, index)=>
                    <div key={index} className="letter" id={`alphabet-${letter}`} onClick={()=>alphabetFilter(letter)}>{letter}</div>
                )}
            </div>
            {/* description result */}
            {
                resultDesc.length ?
                    <div className="description-result">
                        {resultDesc.length !== 0 && resultDesc.map((result, index) =>
                            <div className="description-result-card" key={index} >
                                <div className="result-card-header">
                                    <div className="result-card-title">{result.item.title}</div>
                                    {
                                        supported &&
                                        <div className={`result-card-audio-icon ${speaking && result.item.title === speakingTitle && 'card-audio-playing'}`} onClick={() => triggerSpeak(result.item.title, result.item.desc)} >
                                            <img src="/verify/img/glossary-icons/audio-icon.svg" />
                                        </div>
                                    }
                                </div>
                                <div className="result-card-desc">{textProcessing(result.item.desc)}</div>
                                {
                                    result.item.link.length > 0 &&
                                    <div>To know more, see <Link to={result.item.link}>{result.item.title}</Link></div>
                                }
                            </div>
                        )}
                    </div>
                :
                    <div className="description-result">
                        {initialDec.length !== 0 && initialDec.map((result, index) =>
                            <div className="description-result-card" key={index} >
                                <div className="result-card-header">
                                    <div className="result-card-title">{result.title}</div>
                                    {
                                        supported &&
                                        <div className={`result-card-audio-icon ${speaking && result.title === speakingTitle && 'card-audio-playing'}`} onClick={() => triggerSpeak(result.title, result.desc)} >
                                            <img src="/verify/img/glossary-icons/audio-icon.svg" />
                                        </div>
                                    }
                                </div>
                                <div className="result-card-desc">{textProcessing(result.desc)}</div>
                                {
                                    result.link.length > 0 &&
                                    <div>To know more, see <Link to={result.link}>{result.title}</Link></div>
                                }

                            </div>
                        )}
                    </div>
            }
            {
                initialDec.length == 0 && resultDesc.length == 0 && <h2 className="no-result-text">No Result</h2>

            }
        </div>
    </div>
    </Layout>
  );
};
