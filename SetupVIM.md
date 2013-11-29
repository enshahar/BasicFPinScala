# Setup VIM for scala

# Vundle

1. 아래 명령어 실행
```
git clone https://github.com/gmarik/vundle.git ~/.vim/bundle/vundle
```
2. .vimrc 설정
```
set nocompatible              " be iMproved
filetype off                  " required!

set rtp+=~/.vim/bundle/vundle/
call vundle#rc()

" let Vundle manage Vundle
" required!
Bundle 'gmarik/vundle'

" My bundles here:

filetype plugin indent on     " required!
"
" Vundle Brief help
" :BundleList          - list configured bundles
" :BundleInstall(!)    - install (update) bundles
" :BundleSearch(!) foo - search (or refresh cache first) for foo
" :BundleClean(!)      - confirm (or auto-approve) removal of unused bundles
"
" see :h vundle for more details or wiki for FAQ
" NOTE: comments after Bundle commands are not allowed.
```

3. 
