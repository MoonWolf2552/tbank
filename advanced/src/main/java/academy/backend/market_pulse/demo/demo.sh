cd /Users/al.v.kovalev/IdeaProjects/market-pulse-hse || exit
export CP="target/classes:$HOME/.m2/repository/info/picocli/picocli/4.7.7/picocli-4.7.7.jar"
export M="academy.backend.market_pulse.demo.Main"

#java -cp "$CP" $M --help
# java -cp "$CP" $M list
# java -cp "$CP" $M list --type STOCK
# java -cp "$CP" $M add STOCK GAZP Газпром RUB
# java -cp "$CP" $M search FXUS