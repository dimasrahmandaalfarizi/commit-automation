$commits = git rev-list --reverse 9c9ecaa070d3f463b303429410b10d6252652383..main
$parent = "ed8346a351ae6c60daf2fc4564ce4a84daca8a24"

foreach ($commit in $commits) {
    $env:GIT_AUTHOR_NAME = (git show -s --format="%an" $commit)
    $env:GIT_AUTHOR_EMAIL = (git show -s --format="%ae" $commit)
    $env:GIT_AUTHOR_DATE = (git show -s --format="%ad" $commit)
    $env:GIT_COMMITTER_NAME = (git show -s --format="%cn" $commit)
    $env:GIT_COMMITTER_EMAIL = (git show -s --format="%ce" $commit)
    $env:GIT_COMMITTER_DATE = (git show -s --format="%cd" $commit)
    
    $tree = (git show -s --format="%T" $commit)
    $msg = (git show -s --format="%B" $commit)
    
    $msg_file = "msg_$commit.txt"
    [System.IO.File]::WriteAllText($msg_file, $msg, (New-Object System.Text.UTF8Encoding($False)))
    
    $parent = (git commit-tree $tree -p $parent -F $msg_file)
    Remove-Item $msg_file
}

Write-Output "Final commit is: $parent"
git branch -f main $parent