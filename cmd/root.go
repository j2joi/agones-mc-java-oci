package cmd

import (
	"fmt"
	"os"
	"piday/agones-mc-oci/internal/config"
	"piday/agones-mc-oci/internal/log"

	"github.com/spf13/cobra"
	"go.uber.org/zap"
)

var logger *zap.Logger

var RootCmd = &cobra.Command{
	Use: "agones-mc-oci",
	PersistentPreRunE: func(cmd *cobra.Command, args []string) error {
		var err error
		logger, err = log.NewLogger(config.NewSharedConfig().GetEnvironment(), config.Subcommand(cmd.Name()))
		return err
	},
	PersistentPostRunE: func(cmd *cobra.Command, args []string) error {
		return logger.Sync()
	},
}

func Execute() {
	if err := RootCmd.Execute(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}
