def map(text): 
            d = {} 

            # counting number of times each word comes up in list of words (in dictionary)
            for word in text.split(): 
                d[word] = d.get(word, 0) + 1
            return d
