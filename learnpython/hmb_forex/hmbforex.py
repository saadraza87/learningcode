from urllib.request import urlopen
from bs4 import BeautifulSoup

url = ("http://www.habibmetro.com/exchange-rates")

# using try-except to check if URL is being
try:
	page = urlopen(url)
except:
	print("Error opening the URL")

soup = BeautifulSoup(page, 'html.parser')

content = soup.find('div', 
{"class":"wpb_wrapper"})

rates =  ''

for i in content.findAll('td'):
	rates = rates + ' ' + i.text
with open('scraped_text.txt', 'w') as file:
	file.write(rates)